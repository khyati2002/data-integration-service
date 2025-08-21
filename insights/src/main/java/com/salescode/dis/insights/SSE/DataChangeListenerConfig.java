package com.salescode.dis.insights.SSE;

import com.salescode.dis.insights.SSE.DataChangeListener;
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