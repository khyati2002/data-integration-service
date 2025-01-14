package com.salescode.channelkart.event.engine;

import com.applicate.services.channelkart.response.OperationStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

public class EventListenerResponse {

   private String listenerName;

   private OperationStatus operationStatus;

   private Map<String, EventConsumerDetails> consumerDetails;

   public String getListenerName() {
      return listenerName;
   }

   private int getCurrentTry() {
      return getConsumerDetails().size();
   }

   public boolean shouldRetry() {
      return operationStatus == OperationStatus.Failure;
   }

   public static EventListenerResponse createFrom(EventConsumerDetails details) {
      return new EventListenerResponse()
              .setOperationStatus(details.getStatus())
              .setListenerName(details.getListenerName())
              .addConsumerDetails(details);
   }

   public EventListenerResponse addConsumerDetails(EventConsumerDetails details) {
      this.operationStatus = details.getStatus().isSuccess() ? OperationStatus.Success : OperationStatus.Failure;
      getConsumerDetails()
              .put("eventConsumeOperation-" + (getCurrentTry() + 1), details);
      return this;
   }

   public EventListenerResponse setListenerName(String listenerName) {
      this.listenerName = listenerName;
      return this;
   }

   public OperationStatus getOperationStatus() {
      return operationStatus;
   }

   public EventListenerResponse setOperationStatus(OperationStatus operationStatus) {
      this.operationStatus = operationStatus;
      return this;
   }

   @JsonIgnore
   public Map<String, EventConsumerDetails> getConsumerDetails() {
      if (consumerDetails == null) {
         this.consumerDetails = new HashMap<>();
      }
      return consumerDetails;
   }

   public EventListenerResponse setConsumerDetails(Map<String, EventConsumerDetails> consumerDetails) {
      this.consumerDetails = consumerDetails;
      return this;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder
              .append("EventListenerResponse{")
              .append("listenerName='").append(listenerName)
              .append(", operationStatus=").append(operationStatus.toString());
      if (operationStatus != OperationStatus.Success){
         builder.append(", consumerDetails=").append(consumerDetails.toString());
      }
      builder.append("}");
      return builder.toString();
   }
}