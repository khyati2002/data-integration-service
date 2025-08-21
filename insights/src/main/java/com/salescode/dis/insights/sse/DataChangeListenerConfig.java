package com.salescode.dis.insights.sse;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataChangeListenerConfig {

    @Bean
    public DataChangeListener dataChangeListener(ApplicationEventPublisher eventPublisher) {
        return new DataChangeListener(eventPublisher);
    }
}