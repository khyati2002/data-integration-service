package com.salescode.dis.insights.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic fileUpdatesTopic() {
        // Creates the topic if it doesn't exist, with 5 partitions and replication factor of 1
        return TopicBuilder.name("file-updates")
                .partitions(5)
                .replicas(1)
                .build();
    }
}