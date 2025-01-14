package com.salescode.channelkart.event.sqs;

/**
 * @author : Jinu
 * Date    : 7/27/2020
 **/
public class SqsQueueConfiguration {

   private String queueUrl;

   private String deadLetterQueueUrl;

   public SqsQueueConfiguration(String queueUrl, String deadLetterQueueUrl) {
      this.queueUrl = queueUrl;
      this.deadLetterQueueUrl = deadLetterQueueUrl;

   }

   public String getQueueUrl() {
      return this.queueUrl;
   }

   public String getDeadLetterQueueUrl() {
      return deadLetterQueueUrl;
   }

   @Override
   public String toString() {
      return "SqsQueueConfiguration{" +
              "queueUrl='" + queueUrl + '\'' +
              ", deadLetterQueueUrl='" + deadLetterQueueUrl + '\'' +
              '}';
   }
}
