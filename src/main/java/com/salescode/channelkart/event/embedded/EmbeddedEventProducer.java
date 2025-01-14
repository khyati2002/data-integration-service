package com.salescode.channelkart.event.embedded;

import com.salescode.channelkart.event.Event;
import com.salescode.channelkart.event.EventProducer;
import com.salescode.channelkart.event.engine.EventEngine;
import com.salescode.channelkart.event.engine.EventResponse;
import com.salescode.channelkart.event.provider.EmbeddedEventProfile;
import com.salescode.channelkart.logging.EventLogger;
import com.salescode.channelkart.models.UserContext;
import com.salescode.channelkart.security.SecurityContextUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author : Jinu
 * Date    : 7/29/2020
 **/
public class EmbeddedEventProducer implements EventProducer {

   private static final Logger log = LoggerFactory.getLogger(EmbeddedEventProducer.class);

   private final ExecutorService executorService;

   private final EventEngine eventEngine;

   private final EventLogger eventLogger;

   private final EmbeddedEventProfile eventProfile;

   public EmbeddedEventProducer(EmbeddedEventProfile profile, ExecutorService executorService, EventEngine eventEngine, EventLogger eventLogger) {
      this.eventProfile = profile;
      this.executorService = executorService;
      this.eventEngine = eventEngine;
      this.eventLogger = eventLogger;
   }

   @Override
   public <T> String send(Event<T> event) {
      sendEvent(event);
      return event.getId();
   }

   private <T> void sendEvent(Event<T> convertedEvent) {
      log.debug("Processing event {}", convertedEvent.getId());
      UserContext userContext = SecurityContextUtils.getUserContext()
              .orElseGet(() -> new UserContext(convertedEvent.getUserName(), convertedEvent.getLob()));
      executorService.submit(()-> SecurityContextUtils.switchWithUser(userContext, () -> {
         long startingTime = System.currentTimeMillis();
         log.info("starting the event processing for event topic:{} at {}", convertedEvent.getTopic(), startingTime);
         if (executorService instanceof ThreadPoolExecutor) {
            int currentTaskSize = ((ThreadPoolExecutor) executorService).getQueue().size();
            log.info("Current event embedded queue size is :{}", currentTaskSize);
         }
         try{
            executeWithRetry(convertedEvent);
         }catch (Exception e){
            failureResponse(convertedEvent, e);
         } finally {
            long endingTime = System.currentTimeMillis();
            long timeTaken = (endingTime - startingTime);
            log.info("ending the event processing for event topic:{} at {} and it took {} milliseconds", convertedEvent.getTopic(), endingTime, timeTaken);
            if (timeTaken > 10000) {
               log.error("FATAL ISSUE: this event took more than 10 seconds: topic:{}, lob:{}, timeTaken:{} seconds", convertedEvent.getTopic(), convertedEvent.getLob(), timeTaken);
            }
         }
      }));
   }

   private <T> EventResponse executeWithRetry(Event<T> convertedEvent) {
     return eventEngine.executeWithRetryEvent(convertedEvent, eventProfile.getMaximumRetry());
   }

   private EventResponse failureResponse(Event<?> event, Throwable e) {
      UserContext context = UserContext.from(event.getUserName(), event.getLob());
      String error = ExceptionUtils.getRootCauseMessage(e);
      SecurityContextUtils.switchWithUser(context, ()-> eventLogger.logEventFailed(event, error));
      log.error("Exception thrown .. for {} got {}", event.getId(), error);
      return EventResponse.createFrom(event.getId(),e);
   }
}
