package com.salescode.channelkart.event.sqs;

import com.salescode.channelkart.event.type.CommonEventProfile;

import java.util.Objects;

public class SqsEventProfile extends CommonEventProfile {

   private String queueName;

   private String awsAccessKey;

   private String awsSecretKey;

   private String region;

   private int maximumThreads;

   public String getQueueName() {
      return queueName;
   }


   public SqsEventProfile setQueueName(String queueName) {
      this.queueName = queueName;
      return this;
   }

   public String getAwsAccessKey() {
      return awsAccessKey;
   }

   public SqsEventProfile setAwsAccessKey(String awsAccessKey) {
      this.awsAccessKey = awsAccessKey;
      return this;
   }

   public String getAwsSecretKey() {
      return awsSecretKey;
   }

   public SqsEventProfile setAwsSecretKey(String awsSecretKey) {
      this.awsSecretKey = awsSecretKey;
      return this;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof SqsEventProfile)) return false;

      SqsEventProfile that = (SqsEventProfile) o;

      if (!Objects.equals(queueName, that.queueName)) return false;
      if (!Objects.equals(awsAccessKey, that.awsAccessKey)) return false;
      return Objects.equals(awsSecretKey, that.awsSecretKey);
   }

   public String getRegion() {
      return region;
   }

   public SqsEventProfile setRegion(String region) {
      this.region = region;
      return this;
   }

   @Override
   public int hashCode() {
      int result = queueName != null ? queueName.hashCode() : 0;
      result = 31 * result + (awsAccessKey != null ? awsAccessKey.hashCode() : 0);
      result = 31 * result + (awsSecretKey != null ? awsSecretKey.hashCode() : 0);
      return result;
   }

   public int getMaximumThreads() {
      return maximumThreads;
   }

   public SqsEventProfile setMaximumThreads(int maximumThreads) {
      this.maximumThreads = maximumThreads;
      return this;
   }
}
