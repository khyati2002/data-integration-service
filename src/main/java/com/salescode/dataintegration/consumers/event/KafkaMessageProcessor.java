package com.salescode.dataintegration.consumers.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.salescode.dataintegration.database.DatabaseService;

@Service
public class KafkaMessageProcessor {
    private final ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public KafkaMessageProcessor() {
        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(50);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.initialize();
    }

    @Transactional
    public void processMessage(String message, Acknowledgment acknowledgment) {
        taskExecutor.execute(() -> {
            kafkaTemplate.executeInTransaction(operations -> {
                try {
                    databaseService.insertData(message);
                    acknowledgment.acknowledge();
                }
                catch(final Exception e){
                    //Add error log
                } 
                return null;
            });
        });
    }
}