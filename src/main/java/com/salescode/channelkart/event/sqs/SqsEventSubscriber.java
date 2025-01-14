package com.salescode.channelkart.event.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.applicate.services.channelkart.event.engine.EventEngine;
import com.applicate.services.channelkart.event.engine.EventResponse;
import com.applicate.services.channelkart.event.queue.message.DefaultQueueMessageConverter;
import com.applicate.services.channelkart.event.queue.message.QueueMessage;
import com.applicate.services.channelkart.event.queue.message.QueueMessageConverters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author : Jinu
 * Date    : 7/12/2020
 **/
public class SqsEventSubscriber {

   private static final Logger log = LoggerFactory.getLogger(SqsEventSubscriber.class);

   private final EventEngine eventEngine;

   private final ExecutorService sqsExecutorService;

   private final AmazonSQS sqsClient;

   private final SqsEventProfile eventProfile;

   private final SqsQueueConfiguration queueConfiguration;

   private final QueueMessageConverters converters;

   private final AtomicBoolean isStarted;

   public SqsEventSubscriber(EventEngine eventEngine, AmazonSQS sqsClient, SqsEventProfile eventProfile, SqsQueueConfiguration queueConfiguration, QueueMessageConverters converters) {
      this.eventEngine = eventEngine;
      this.sqsClient = sqsClient;
      this.eventProfile = eventProfile;
      this.queueConfiguration = queueConfiguration;
      this.converters = converters;
      this.sqsExecutorService = createExecutorService(eventProfile);
      isStarted = new AtomicBoolean(false);
      start();
   }

   private void start() {
      isStarted.set(true);
      sqsExecutorService.submit(this::runSafely);
      log.info("Sqs Event consumer started...");
   }

   private void runSafely() {
      while (isStarted.get()) {
         try {
            log.trace("polling sqs messages from infinite loop...");
            pollMessages();
         } catch (Exception e) {
            log.error("Unhandled exception happened from sqs event subscriber", e);
         }
      }
   }

   private ExecutorService createExecutorService(SqsEventProfile eventProfile) {
      return Executors.newFixedThreadPool(eventProfile.getMaximumThreads(), new CustomizableThreadFactory("sqs-event-pool-"));
   }

   public void shutDown() {
      this.isStarted.set(false);
      if (this.sqsExecutorService != null) {
         this.sqsExecutorService.shutdown();
      }
   }

   public void pollMessages() {
      try {
         log.trace("pulling messages from sqs queue:{}", queueConfiguration.getQueueUrl());
         ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueConfiguration.getQueueUrl())
                 .withAttributeNames("All")
                 .withMessageAttributeNames("All")
                 .withWaitTimeSeconds(20)
                 .withMaxNumberOfMessages(eventProfile.getMaximumThreads())
                 .withVisibilityTimeout(43200);
         ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(receiveMessageRequest);
         receiveMessageResult.getMessages()
                 .forEach(this::sendAsync);
      } catch (Exception e) {
         log.error("FATAL_ERROR: unhandled exception happened in sqs subscriber.... Please check the sqs configurations", e);
      }
   }

   private void sendAsync(Message message) {
      log.debug("passing event message to event engine: {}", message);
      CompletableFuture.supplyAsync(() -> toQueueMessage(message), sqsExecutorService)
              .thenApplyAsync(queueMessage -> converters.get(queueMessage).toEvent(queueMessage), sqsExecutorService)
              .thenApplyAsync(e -> eventEngine.executeWithRetryEvent(e,eventProfile.getMaximumRetry()), sqsExecutorService)
              .exceptionally(throwable -> handleException(throwable, message))
              .thenAcceptAsync(eventResponse -> handleResponse(message, eventResponse), sqsExecutorService);
   }

   private EventResponse handleException(Throwable throwable, Message message) {
      log.error("unhandled exception happened while consuming the event for id:{}", message.getMessageId(), throwable);
      return EventResponse.createFrom(message.getMessageId(), throwable);
   }

   private void handleResponse(Message message, EventResponse eventResponse) {
      if (eventResponse.getOperationStatus().isSuccess()) {
         sqsClient.deleteMessage(queueConfiguration.getQueueUrl(), message.getReceiptHandle());
      } else {
         int delay = (int) TimeUnit.MILLISECONDS.toSeconds(eventProfile.getRetryDelay());
         sqsClient.changeMessageVisibility(queueConfiguration.getQueueUrl(), message.getReceiptHandle(), delay);
      }
   }

   private int getCurrentTryCount(Message message) {
      String approximateReceiveCount = message.getAttributes().get("ApproximateReceiveCount");
      return Integer.parseInt(approximateReceiveCount) - 1;
   }

   private QueueMessage toQueueMessage(Message message) {
      return new QueueMessage()
              .setMessage(message.getBody())
              .setAttributes(getAttributes(message));
   }

   private Map<String, String> getAttributes(Message message) {
      Map<String, String> attributes = new HashMap<>(message.getAttributes());
      message.getMessageAttributes()
              .forEach((key, value) -> attributes.put(key, value.getStringValue()));
      int count = getCurrentTryCount(message);
      attributes.put(DefaultQueueMessageConverter.CURRENT_RETRY_COUNT, String.valueOf(count));
      attributes.compute(DefaultQueueMessageConverter.MAX_RETRY_COUNT, (key, value) -> computeMaximumRetryValue(value));
      return attributes;
   }

   private String computeMaximumRetryValue(String value) {
      int max = 0;
      if (value != null) {
         max = Integer.parseInt(value);
      }
      if (max <= 0) {
         max = eventProfile.getMaximumRetry();
      }
      return String.valueOf(max);
   }

}