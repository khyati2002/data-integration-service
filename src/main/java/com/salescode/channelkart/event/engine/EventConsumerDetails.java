package com.salescode.channelkart.event.engine;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.salescode.channelkart.models.EventListenerInfo;
import com.salescode.channelkart.response.OperationStatus;

import java.util.Date;

/**
 * @author : Jinu
 * Date    : 7/27/2020
 **/
public class EventConsumerDetails {


   @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSSSSSZ")
   private Date consumedAt;

   private OperationStatus status;

   private String message;

   private String listenerName;

   public Date getConsumedAt() {
      return consumedAt;
   }

   public String getListenerName() {
      return listenerName;
   }

   public EventConsumerDetails setListenerName(String listenerName) {
      this.listenerName = listenerName;
      return this;
   }

   public OperationStatus getStatus() {
      return status;
   }

   public String getMessage() {
      return message;
   }

   public EventConsumerDetails setConsumedAt(Date consumedAt) {
      this.consumedAt = consumedAt;
      return this;
   }

   public EventConsumerDetails setStatus(OperationStatus status) {
      this.status = status;
      return this;
   }

   public EventConsumerDetails setMessage(String message) {
      this.message = message;
      return this;
   }

   public static EventConsumerDetails createFrom(EventListenerInfo info, OperationStatus status, String message) {
      return new EventConsumerDetails()
              .setStatus(status)
              .setConsumedAt(new Date())
              .setMessage(message)
              .setListenerName(info.getImplementation());
   }

   @Override
   public String toString() {
      return "EventConsumerDetails{" +
              "consumedAt=" + consumedAt +
              ", status=" + status +
              ", message='" + message + '\'' +
              ", listenerName='" + listenerName + '\'' +
              '}';
   }
}
