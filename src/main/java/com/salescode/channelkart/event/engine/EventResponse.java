package com.salescode.channelkart.event.engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.salescode.channelkart.response.OperationStatus;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventResponse {

   private String eventId;

   private OperationStatus operationStatus;

   private String message;

   private Map<String, EventListenerResponse> listenerResponses;

   public OperationStatus getOperationStatus() {
      if (operationStatus == null) {
         this.operationStatus = OperationStatus.Success;
      }
      return operationStatus;
   }


   public static EventResponse createFrom(EventListenerResponse response) {
      OperationStatus operationStatus = response.getOperationStatus();
      return new EventResponse()
              .setOperationStatus(operationStatus)
              .addListenerResponse(response);
   }

   public static EventResponse createFrom(String eventId, Throwable e) {
      return new EventResponse()
              .setEventId(eventId)
              .setOperationStatus(OperationStatus.Failure)
              .setMessage(e.getMessage());
   }

   public EventResponse addListenerResponse(EventListenerResponse listenerResponse) {
      getListenerResponses().put(listenerResponse.getListenerName(), listenerResponse);
      return this;
   }

   public EventResponse setIfNotPresent(String message) {
      if (this.message == null) {
         this.message = message;
      }
      return this;
   }

   public EventResponse setOperationStatus(OperationStatus operationStatus) {
      this.operationStatus = operationStatus;
      return this;
   }

   public EventResponse setOperationStatus(){
      this.operationStatus = anyFailed() ? OperationStatus.Failure : OperationStatus.Success;
      return this;
   }

   private boolean anyFailed() {
      return getListenerResponses()
              .values()
              .stream()
              .anyMatch(response -> response.getOperationStatus() == OperationStatus.Failure);
   }

   public String getMessage() {
      return message;
   }

   public EventResponse setMessage(String message) {
      this.message = message;
      return this;
   }

   public Map<String, EventListenerResponse> getListenerResponses() {
      if (listenerResponses == null) {
         this.listenerResponses = new LinkedHashMap<>();
      }
      return listenerResponses;
   }

   public EventResponse setListenerResponses(Map<String, EventListenerResponse> listenerResponses) {
      this.listenerResponses = listenerResponses;
      return this;
   }

   public String getEventId() {
      return eventId;
   }

   public EventResponse setEventId(String eventId) {
      this.eventId = eventId;
      return this;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder
              .append("EventResponse{")
              .append("eventId='").append(eventId).append('\'')
              .append(", operationStatus=").append(operationStatus.toString())
              .append(", message='").append(message).append('\'');
      if(operationStatus!=OperationStatus.Success){
         builder.append(", listenerResponses=").append(listenerResponses.toString());
      }
      builder.append("}");
      return builder.toString();
   }


}
