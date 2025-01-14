package com.salescode.channelkart.event.sqs;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.channelkart.event.Event;
import com.salescode.channelkart.event.EventProducer;
import com.salescode.channelkart.event.type.EventProviderType;
import com.salescode.channelkart.logging.EventLogger;
import com.salescode.channelkart.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author : Jinu
 * Date    : 7/11/2020
 **/
public class SqsEventProducer implements EventProducer {

   private static final Logger log = LoggerFactory.getLogger(SqsEventProducer.class);

   private final EventLogger eventLogger;

   private final AmazonSQS sqsClient;

   private final QueueMessageConverters messageConverters;

   private final SqsQueueConfiguration queueConfiguration;

   public SqsEventProducer(EventLogger eventLogger, SqsQueueConfiguration queueConfiguration,  AmazonSQS sqsClient, QueueMessageConverters messageConverters) {
      this.eventLogger = eventLogger;
      this.sqsClient = sqsClient;
      this.queueConfiguration = queueConfiguration;
      this.messageConverters = messageConverters;
   }

   @Override
   public <T> String send(Event<T> event) {
      log.debug("Sending message:{} to queue:{} ", event, queueConfiguration.getQueueUrl());
      try {
         Task eventTask = eventLogger.logEventTask(event, EventProviderType.SQS);
         QueueMessage queueMessage = messageConverters.get(event).toMessage(event);
         ObjectNode jsonNode = eventTask.getAttributes().deepCopy();
         jsonNode.put("data", JSONUtils.getObjectMapper().writeValueAsString(queueMessage));
         eventTask.setAttributes(jsonNode);
         eventLogger.updateEventTask(eventTask);
         String randomId = UUID.randomUUID().toString();
         SendMessageRequest sendMessageRequest = new SendMessageRequest()
                 .withQueueUrl(queueConfiguration.getQueueUrl())
                 .withMessageBody(queueMessage.getMessage())
                 .withMessageGroupId("channelkart-message-group" + randomId)
                 .withMessageAttributes(getMessageAttributes(queueMessage))
                 .withMessageDeduplicationId(Objects.hash(event) + randomId + "" + System.currentTimeMillis());
         SendMessageResult sendMessageResult = sqsClient.sendMessage(sendMessageRequest);
         log.debug("Event with id :{}, successfully submitted to aws with aws-message-id:{}", event.getId(), sendMessageResult.getMessageId());
         return event.getId();
      } catch (Exception e) {
         if (event.getId() != null) {
            eventLogger.logEventFailed(event, e.getMessage());
         }
         throw new EventException("Could not send event", e);
      }
   }

   private Map<String, MessageAttributeValue> getMessageAttributes(QueueMessage message) {
      return message.getAttributes()
              .entrySet()
              .stream()
              .collect(Collectors.toMap(Map.Entry::getKey, entry -> createAttribute(entry.getValue())));
   }

   private MessageAttributeValue createAttribute(String value) {
      if (value == null) {
         value = "unknown";
      }
      return new MessageAttributeValue()
              .withStringValue(value)
              .withDataType("String");
   }

}
