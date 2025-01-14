package com.salescode.channelkart.event.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : Jinu
 * Date    : 7/27/2020
 **/

public class SqsQueueBuilder {

   private static final Logger log = LoggerFactory.getLogger(SqsQueueBuilder.class);

   private AmazonSQS sqs;

   public SqsQueueBuilder(AmazonSQS sqs) {
      this.sqs = sqs;
   }

   public SqsQueueConfiguration build(SqsEventProfile profile) {
      String queueName = profile.getQueueName();
      String queueUrl = createFifoQueue(queueName, sqs);
      String deadLetterQueueName = "dead-letter-" + queueName;
      String deadLetterQueueUrl = createFifoQueue(deadLetterQueueName, sqs);
      bindDeadLetterQueue(profile, queueUrl, deadLetterQueueUrl);
      return new SqsQueueConfiguration(queueUrl, deadLetterQueueUrl);
   }

   private void bindDeadLetterQueue(SqsEventProfile eventProfile, String queueUrl, String deadLetterQueueUrl) {
      GetQueueAttributesResult deadLetterQueueAttributes = sqs.getQueueAttributes(
              new GetQueueAttributesRequest(deadLetterQueueUrl)
                      .withAttributeNames("QueueArn"));
      String deadLetterQueueArn = deadLetterQueueAttributes.getAttributes().get("QueueArn");
      // Set dead letter queue with redrive policy on source queue.
      int eventRetryCount = eventProfile.getMaximumRetry();
      log.info("Binding deadletter queue {} with main queue{} with maximum retry limit '{}'", deadLetterQueueUrl, queueUrl, eventRetryCount);
      SetQueueAttributesRequest request = new SetQueueAttributesRequest()
              .withQueueUrl(queueUrl)
              .addAttributesEntry("RedrivePolicy",
                      "{\"maxReceiveCount\":\"" + (eventRetryCount + 1) + "\", \"deadLetterTargetArn\":\""
                              + deadLetterQueueArn + "\"}");

      sqs.setQueueAttributes(request);
   }

   private String createFifoQueue(String queueName, AmazonSQS sqs) {
      Map<String, String> queueAttributes = new HashMap<>();
      queueAttributes.put("FifoQueue", "true");
      queueAttributes.put("ContentBasedDeduplication", "false");
      CreateQueueRequest createFifoQueueRequest = new CreateQueueRequest(
              queueName + ".fifo").withAttributes(queueAttributes);
      return createQueueIfNotExist(createFifoQueueRequest, sqs);
   }

   private String createQueueIfNotExist(CreateQueueRequest request, AmazonSQS sqs) {
      try {
         String queueUrl = sqs.createQueue(request).getQueueUrl();
         log.info("Sqs QueryUrl created {}", queueUrl);
         return queueUrl;
      } catch (AmazonSQSException e) {
         if (!e.getErrorCode().equals("QueueAlreadyExists")) {
            throw e;
         }
      }
      return sqs.getQueueUrl(request.getQueueName()).getQueueUrl();
   }
}
