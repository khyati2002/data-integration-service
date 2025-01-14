package com.salescode.channelkart.services;

import com.salescode.channelkart.dto.StreamingEventData;
import com.salescode.channelkart.event.CdmEventData;
import com.salescode.channelkart.event.Event;
import com.salescode.channelkart.event.EventBuilder;
import com.salescode.channelkart.event.EventTopic;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.models.EventListenerInfo;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.services.enums.EntityOperation;
import com.salescode.channelkart.utils.EventUtil;
import com.salescode.dataintegration.etl.metadata.registry.EventListenerRegistry;
import com.salescode.dataintegration.etl.metadata.registry.EventProducerFactory;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CdmPostEventService {

    private static final Logger log = LoggerFactory.getLogger(CdmPostEventService.class);

    private final EventListenerRegistry eventListenerRegistry;

    private final EventProducerFactory producerFactory;

    public CdmPostEventService(EventListenerRegistry eventListenerRegistry, EventProducerFactory producerFactory) {
        this.eventListenerRegistry = eventListenerRegistry;
        this.producerFactory = producerFactory;
    }


    public void onPostEvent(StreamingEventData<List<CommonDataModel>> eventData) {
        try {
            List<CommonDataModel> dataModels = (List<CommonDataModel>) eventData.retreiveObject();
            if(dataModels == null || dataModels.isEmpty()){
                throw new SerializationException("Unable to serialize :" + eventData.getClazz());
            }
            EntityOperation operation = eventData.getOperation();
            String lobForThisEvent = eventData.getLob();
            if(lobForThisEvent==null || lobForThisEvent.isEmpty()){
                throw new IllegalStateException("Unknown state where Lob is null or empty for Event " + eventData.getRequestId());
            }
            List<EventListenerInfo> matchingListenerConfig =
                    EventUtil.findMatchingListenerConfig(dataModels.get(0), operation, eventListenerRegistry.get(lobForThisEvent, true), eventData.getEventTopics());

            if(!matchingListenerConfig.isEmpty()) {
                log.debug("onPostEvent matchingListenerConfig {}", matchingListenerConfig);
                matchingListenerConfig.stream()
                        .map(info -> createEvent(info, dataModels, operation, eventData.getRequestId()))
                        .forEach(event -> {
                            log.info("Going to send the event got from kafka to embedded processor. Topic:{}, lob:{}", event.getTopic(), event.getLob());
                            producerFactory.getDefault().send(event);
                        });
            }
        } catch (Exception e) {
            log.error("Could not process post event for CDM's:{} with operation: {}", eventData.getRequestId(), eventData.getOperation(), e);
        }

    }

    private Event<CdmEventData> createEvent(EventListenerInfo info, List<? extends CommonDataModel> dataModels, EntityOperation operation, String id) {
        id = id + RandomStringUtils.randomAlphanumeric(10);
        CommonDataModel dataModel = dataModels.get(0);
        List<String> identifiers = dataModels.stream().map(CommonDataModel::getId).collect(Collectors.toList());
        CdmEventData data = new CdmEventData()
                .setCdmClazz((Class<CommonDataModel>) dataModel.getClass())
                .setCdmIdentifiers(identifiers)
                .setModels((List<CommonDataModel>) dataModels)
                .setOperation(operation);
        data.setModels((List<CommonDataModel>) dataModels);
        return new EventBuilder<>(CdmEventData.class)
                .withTopic(EventTopic.of(info.getTopic()))
                .withData(data)
                .fromUser(SecurityContextUtils.getPrincipal())
                .withLob(SecurityContextUtils.getLob())
                .buildWithId(id);
    }

}
