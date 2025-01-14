package com.salescode.channelkart.event.engine;

import com.salescode.channelkart.event.Event;
import com.salescode.channelkart.event.EventListener;
import com.salescode.channelkart.event.EventListeners;
import com.salescode.channelkart.logging.EventLogger;
import com.salescode.channelkart.models.EventListenerInfo;
import com.salescode.channelkart.models.UserContext;
import com.salescode.channelkart.response.OperationStatus;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.dataintegration.etl.metadata.registry.EventListenerRegistry;
import com.salescode.dis.FlinkApplication;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciithemes.a7.A7_Grids;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EventEngine {

   private static final Logger log = LoggerFactory.getLogger(EventEngine.class);

   private final EventListeners eventListeners;

   private final EventListenerRegistry eventListenerRegistry;

   private final EventLogger eventLogger;


   public EventEngine(EventListeners eventListeners, EventListenerRegistry eventListenerRegistry, EventLogger eventLogger) {
      this.eventListeners = eventListeners;
      this.eventListenerRegistry = eventListenerRegistry;
      this.eventLogger = eventLogger;
   }

   private <T> EventConsumerDetails notifyListener(EventListenerInfo info, Event<T> event) {
      try {
         EventListener<T> listener = eventListeners.get(info);
         listener.listen(event, info);
         return EventConsumerDetails.createFrom(info, OperationStatus.Success, "successfully consumed");
      } catch (Exception e) {
         log.debug("Event Listener:{} failed to receive the event:{}", info.getImplementation(), event, e);
         sendSlackNotification(e, info, event);
         return EventConsumerDetails.createFrom(info, OperationStatus.Failure, ExceptionUtils.getMessage(e));
      }
   }

   private <T> void sendSlackNotification(Exception e, EventListenerInfo info, Event<T> event) {
      try {
         String description = createNotificationBody(e, info, event);
         NotifyEventAdapter.notifyEventEngine(NotifyEventChannel.EVENT_LISTNER, NotifyEventSeverity.CRITICAL, SecurityContextUtils.getLob(), SecurityContextUtils.getLob().toUpperCase(), description, null, NotifyEvent.Status.FAILURE, "");
      } catch (Exception exception) {
         log.error("Could not send slack event failure notification", e);
      }
   }

   public <T> String createNotificationBody(Exception e, EventListenerInfo info, Event<T> event){
      String exception = e.getClass().getSimpleName();
      String exceptionMessage = e.getMessage();
      String env = FlinkApplication.getEnv();
      AsciiTable at = new AsciiTable();
      at.addRule();
      at.addRow( "Environment", env);
      at.addRule();
      at.addRow( "LOB", SecurityContextUtils.getLob());
      at.addRule();
      at.addRow( "Status", NotifyEvent.Status.FAILURE);
      at.addRule();
      at.addRow( "Event Id", event.getId());
      at.addRule();
      at.addRow( "Listener", info.getImplementation());
      at.addRule();
      at.addRow( "Topic", info.getTopic());
      at.addRule();
      getConfiguration(info, "cdm").ifPresent(cdm -> {
         at.addRow( "Model", cdm);
         at.addRule();
      });
      getConfiguration(info, "operation").ifPresent(operation -> {
         at.addRow( "Operation",  operation);
         at.addRule();
      });0-9
      at.addRow( "Exception", exception);
      at.addRule();
      at.addRow( "Message", exceptionMessage);
      at.addRule();
      at.getContext().setGrid(A7_Grids.minusBarPlusEquals());
      return "```"+ at.render(65)+"```";
   }

   private Optional<String> getConfiguration(EventListenerInfo info, String key) {
      if (info.getConfigurations() != null && info.getConfigurations().has(key)) {
         return Optional.of(info.getConfigurations().get(key).asText());
      }
      return Optional.empty();
   }

   public <T> EventResponse executeWithRetryEvent(Event<T> event, int maxRetries) {
      int currentTry = 0;
      EventResponse response = null;
      UserContext context = UserContext.from(event.getUserName(), event.getLob());
      while(currentTry<maxRetries && currentTry < 1){
         response = SecurityContextUtils.switchWithUser(context, () -> executeEvent(event));

         if (response.getOperationStatus().isSuccess()){
            return response;
         }
         currentTry += 1;
         try {
            Thread.sleep(currentTry * 200l);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("can't wait..");
         }
      }
      if (response != null && !response.getOperationStatus().isSuccess()) {
         log.error("Event {} failed for topic:{} reason:{} ", event.getId(), event.getTopic(), response);
         EventResponse finalResponse = response;
         SecurityContextUtils.switchWithUser(context, () -> eventLogger.logEventFailed(event,  finalResponse.toString()));
      }
      return response;
   }

   private <T> EventResponse executeEvent(Event<T> event) {
      List<EventListenerInfo> eventListenerInfos = eventListenerRegistry.get(event.getLob(), event.getTopic().value()).stream().filter(EventListenerInfo::isEnabled).collect(Collectors.toList());
      if (eventListenerInfos==null || eventListenerInfos.isEmpty()) {
         log.warn("Event {} has no listeners", event.getId());
         eventLogger.logEventSuccess(event.getId());
         return new EventResponse()
                 .setEventId(event.getId())
                 .setOperationStatus(OperationStatus.Success)
                 .setMessage("No listeners found for this event..");
      }else{
         EventResponse eventResponse = new EventResponse().setEventId(event.getId());
         sendEvent(event, eventListenerInfos)
                 .stream()
                 .map(EventListenerResponse::createFrom)
                 .forEach(eventResponse::addListenerResponse);
         eventResponse.setOperationStatus();

         if (eventResponse.getOperationStatus().isSuccess()) {
            eventResponse.setMessage("Event consumed Successfully");
            log.debug("Event {} consumed successfully : {}", event, event.getTopic());
            eventLogger.logEventSuccess(event.getId());
         }
         return eventResponse;
      }
   }

   private <T> List<EventConsumerDetails> sendEvent(Event<T> event, List<EventListenerInfo> infos) {
      return infos.stream()
              .map(info -> notifyListener(info, event))
              .collect(Collectors.toList());
   }
}
