package com.salescode.dis.insights.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@Profile("kafka")
public class KafkaConfig {

    @Bean
    public NewTopic fileUpdatesTopic(
            @Value("${file.progress.update.topic:file-progress-updates}") String fileUpdatesTopicName,
            @Value("${kafka.topic.retention.ms:86400000}") String retentionMs
    ) {
        return TopicBuilder.name(fileUpdatesTopicName).partitions(5).replicas(1).config("retention.ms", retentionMs).build();
    }
    @Bean
    public AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }
}