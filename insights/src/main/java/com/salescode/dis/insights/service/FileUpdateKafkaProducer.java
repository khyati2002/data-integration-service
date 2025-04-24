package com.salescode.dis.insights.service;

import com.salescode.dis.insights.events.FileUpdateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class FileUpdateKafkaProducer {
    private static final String TOPIC = "file-updates";
    
    private final KafkaTemplate<String, FileUpdateEvent> kafkaTemplate;
    
    @Autowired
    public FileUpdateKafkaProducer(KafkaTemplate<String, FileUpdateEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void sendFileUpdateEvent(FileUpdateEvent event) {
        kafkaTemplate.send(TOPIC, event.getFileId(), event);
    }
}