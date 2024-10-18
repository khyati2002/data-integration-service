package com.salescode.dataintegration.consumers.event;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.event.ListenerContainerIdleEvent;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
public class KafkaMessageConsumer {

    @Autowired
    private KafkaMessageProcessor messageProcessorService;

    @Autowired
    private ApplicationContext context;


    @KafkaListener(topics = "#{'${app.kafka-topic}'}")
    public void consume(List<String> messages, Acknowledgment acknowledgment) {
        
        messages.forEach(m -> messageProcessorService.processMessage(m,acknowledgment));
    }

    // Handle idle container event to trigger shutdown
    @EventListener
    public void onIdleEvent(ListenerContainerIdleEvent event) {
        if (Boolean.TRUE.equals(context.getEnvironment().getProperty("app.auto-shutdown", Boolean.class))) {
            System.out.println("No messages received for the last 5 minutes. Initiating graceful shutdown...");
            ((ConfigurableApplicationContext) context).close();
            System.exit(0);
        }
    }
}