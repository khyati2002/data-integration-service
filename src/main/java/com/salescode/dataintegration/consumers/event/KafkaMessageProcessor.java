package com.salescode.dataintegration.consumers.event;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.salescode.dataintegration.database.DatabaseService;


@Service
public class KafkaMessageProcessor {

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final Logger log = LoggerFactory.getLogger(KafkaMessageProcessor.class);


    private final ThreadPoolTaskExecutor executor;

    @Autowired
    public KafkaMessageProcessor(ThreadPoolTaskExecutor executor) {
        this.executor = executor;
    }

    @Transactional
    public void processMessage(String message, Acknowledgment acknowledgment) {
        CompletableFuture.runAsync(() -> {
            try {
                databaseService.dbInsert(message + Thread.currentThread().getId());
                acknowledgment.acknowledge();  // Positive acknowledgment
            } catch (Exception e) {
                throw new RuntimeException("Failed to process message", e);
            }
        }, executor).exceptionally(e -> {
            log.error("Exception occurred in asynchronous processing: ", e);
            sendToDeadLetterQueue(message);
            acknowledgment.acknowledge();
            return null;
        });
    }

    private void sendToDeadLetterQueue(String message) {
        try {
            kafkaTemplate.send("dead-letter-topic", message);
            log.info("Message sent to dead-letter topic: " + message);
        } catch (Exception e) {
            log.error("Failed to send message to dead-letter topic: " + e.getMessage());
        }
    }
}