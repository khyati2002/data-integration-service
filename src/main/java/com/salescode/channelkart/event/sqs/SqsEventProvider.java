package com.salescode.channelkart.event.sqs;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.salescode.channelkart.event.EventProducer;
import com.salescode.channelkart.event.EventProfile;
import com.salescode.channelkart.event.provider.EventProvider;
import com.salescode.channelkart.logging.EventLogger;
import com.salescode.channelkart.services.SpringContext;
import org.hibernate.event.spi.EventEngine;

/**
 *
 * INSERT INTO `` (`id`,`active_status`,`active_status_reason`,`created_by`,`creation_time`,`extended_attributes`,`last_modified_time`,`lob`,`modified_by`,`version`,`attributes`,`name`,`type`,`source`,`payload`)
 * VALUES ('default-event-profile','1',NULL,'admin','2020-05-23 14:36:13.000000','{}','2020-05-23 14:36:13.000000',NULL,'admin',0,'{\"region\": \"ap-south-1\", \"isDefault\": \"yes\", \"queueName\": \"test-queue-testing\", \"retryDelay\": 300, \"awsAccessKey\": \"awsAccessKey\", \"awsSecretKey\": \"secretkey\", \"maximumRetry\": 3, \"providerType\": \"SQS\", \"maximumThreads\": 10}','sqs','eventProvider',NULL,NULL);
 * @author : Jinu
 * Date    : 8/28/2020
 **/
public class SqsEventProvider implements EventProvider {

   private final SqsEventProfile profile;

   private final AmazonSQS sqsClient;

   private final SqsQueueConfiguration queueConfiguration;

   private final QueueMessageConverters messageConverters;

   private final SqsEventProducer eventProducer;

   private final SqsEventSubscriber eventSubscriber;

   public SqsEventProvider(SqsEventProfile profile) {
      this.profile = profile;
      this.sqsClient = createSqsClient(profile);
      SqsQueueBuilder builder = new SqsQueueBuilder(this.sqsClient);
      this.queueConfiguration = builder.build(profile);
      this.messageConverters = SpringContext.getBean(QueueMessageConverters.class);
      EventLogger eventLogger = SpringContext.getBean(EventLogger.class);
      this.eventProducer = new SqsEventProducer(eventLogger, queueConfiguration, sqsClient, messageConverters);
      this.eventSubscriber = createSubscriber();
   }

   private SqsEventSubscriber createSubscriber() {
      EventEngine engine = SpringContext.getBean(EventEngine.class);
      return new SqsEventSubscriber(engine, sqsClient, profile, queueConfiguration, messageConverters);
   }


   private AmazonSQS createSqsClient(SqsEventProfile profile) {
      return AmazonSQSClientBuilder.standard()
              .withRegion(profile.getRegion())
              .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                      profile.getAwsAccessKey(), profile.getAwsSecretKey())))
              .build();
   }

   @Override
   public void shutDown() {
      if (sqsClient != null) {
         sqsClient.shutdown();
      }
      if (this.eventSubscriber != null) {
         this.eventSubscriber.shutDown();
      }
   }

   @Override
   public EventProducer getProducer() {
      return this.eventProducer;
   }

   @Override
   public EventProfile getProfile() {
      return this.profile;
   }

   @Override
   public boolean isSameProfile(EventProfile profile) {
      return this.profile.equals(profile);
   }

}
