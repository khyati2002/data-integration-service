package com.salescode.dataintegration.consumers.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KafkaMessageConsumer {

    @Autowired
    private KafkaMessageProcessor executorService;

    @KafkaListener(topics = "${spring.kafka.topic.name}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeMessages(List<String> messages, Acknowledgment acknowledgment) {
        for (String message : messages) {
            executorService.processMessage(message, acknowledgment);
        }
    }
}