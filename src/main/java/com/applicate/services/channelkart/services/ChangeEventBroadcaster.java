package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.dto.StreamingEventData;
import com.applicate.services.channelkart.integration.kafka.publisher.IKafkaConstants;
import com.applicate.services.channelkart.integration.kafka.publisher.KafkaEventPublisher;
import com.applicate.services.channelkart.logging.EventMetrics;
import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.EventListenerInfo;
import com.applicate.services.channelkart.models.UserContext;
import com.applicate.services.channelkart.security.SecurityContextUtils;
import com.applicate.services.channelkart.services.enums.EntityOperation;
import com.applicate.services.channelkart.utils.EventUtil;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dataintegration.etl.metadata.registry.EventListenerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class ChangeEventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(ChangeEventBroadcaster.class);
    private static String logStreamEventTypes = System.getProperty("logStreamEventTypes", "AnalyticInput,UserActivity,Sales");

    private static int activeThreadCount = Integer.parseInt(System.getProperty("activeBroadcasters", "5"));
    private static ArrayBlockingQueue<Runnable> abq = new ArrayBlockingQueue<Runnable>(
            Integer.parseInt(System.getProperty("activeBroadcasterQueueSize", "1000"))) {

        @Override
        public boolean offer(Runnable e) {
            try {
                this.put(e);
            } catch (InterruptedException e1) {
                Thread.currentThread().interrupt();
                e1.printStackTrace();
            }
            return true;
        }
    };
    private final ThreadPoolExecutor cte = new ThreadPoolExecutor(activeThreadCount, activeThreadCount, 120,
            TimeUnit.SECONDS, abq);

    @Autowired
    private CdmEntityListener entityListener;

    private static KafkaEventPublisher publisher;
    private static final Object LOCK = new Object();
    @Value("${channelkart.integration.kafka.events.publish:true}")
    private boolean eventKafkaBroadcast;

    public Future<Boolean> broadcast(CommonDataModel entity, EntityOperation operation) {
        return broadcast(Collections.singletonList(entity), operation, null);
    }

    public Future<Boolean> broadcast(List<? extends CommonDataModel> entities, EntityOperation operation) {
        return broadcast(entities, operation, null);
    }

    public Future<Boolean> broadcast(List<? extends CommonDataModel> entities, EntityOperation operation, Set<String> eventTopics) {
        if(eventKafkaBroadcast && publisher==null){
            synchronized (LOCK){
                if(publisher==null){
                    publisher = new KafkaEventPublisher();
                }
            }
        }
        String lob = SecurityContextUtils.getLob();
        final UserContext uc = new UserContext(SecurityContextUtils.getPrincipal(), lob);
        String streamId = SecurityContextUtils.getCurrentRequestId();
        uc.setStreamId(streamId == null ? UUID.randomUUID().toString() : streamId);
        return cte.submit(() -> {
            try {
                List<EventListenerInfo> infoList = EventListenerRegistry.INSTANCE.get(uc.getLob(), true);
                if (entities != null
                        && !entities.isEmpty()
                        && entities.get(0) != null
                        && EventUtil.isListenerConfigsPresent(entities.get(0), operation, infoList, eventTopics)) {
                    SecurityContextUtils.switchWithUser(
                            uc, () ->{
                                StreamingEventData<List<CommonDataModel>> blob = new StreamingEventData<>((List<CommonDataModel>) entities, operation);
                                blob.setEventTopics(eventTopics);
                                blob.setLob(uc.getLob());
                                blob.setLoginId(uc.getUserName());
                                blob.setRequestId(uc.getStreamId());
                                if (eventKafkaBroadcast) {
                                    publisher.publish(IKafkaConstants.getEventTopicName(uc.getLob()), blob);
                                } else {
//                                    listenFor(blob);
                                    throw new UnsupportedOperationException("Operation not permitted");
                                }
                                log(entities, operation);
                            }
                    );
                }
            } catch (Exception e) {
                log.error("Could not broadcast", e);
            }
            return true;
        });
    }

    private void log(List<? extends CommonDataModel> entities, EntityOperation operation) {
        if (operation == EntityOperation.INSERT) {
            logChangeEvents(entities);
        }
        //Delete has to be handled for Logging,Parking for time being
    }

    public void logChangeEvents(List<? extends CommonDataModel> entities) {
        entities.forEach(e -> createEventMetrics(e, SecurityContextUtils.getPrincipal()));
    }

    private void createEventMetrics(CommonDataModel cdm, String loginId) {
        String type = cdm.getClass().getSimpleName();
        if (logStreamEventTypes.contains(type)) {

                try {
                    EventMetrics e = new EventMetrics();
                    e.setType(type);
                    e.setId(cdm.getId());
                    e.setCreatedBy(loginId);
                    e.setLob(SecurityContextUtils.getLob());
                    e.setEventTime(new Date());
                    e.setPayload(JSONUtils.toJsonNode(cdm));
//                    metricStream.sendAsyncEvent(e);
                } catch (Exception e) {
                    log.error("Could not create event metrics",e );
                }

        }
    }

}
