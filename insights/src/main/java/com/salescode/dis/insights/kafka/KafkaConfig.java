package com.salescode.dis.insights.kafka;

import org.springframework.kafka.support.serializer.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dis.insights.dto.event.FileProgressEvent;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("kafka")
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> baseProps(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // tune other consumer settings as needed (max.poll.interval, max.poll.records, etc.)
        return props;
    }


    @Bean
    public NewTopic fileUpdatesTopic(
            @Value("${file.progress.update.topic:file-progress-updates}") String fileUpdatesTopicName,
            @Value("${kafka.topic.retention.ms:86400000}") String retentionMs
    ) {
        return TopicBuilder.name(fileUpdatesTopicName).partitions(5).replicas(1).config("retention.ms", retentionMs).build();
    }

    @Bean
    public NewTopic orderUpdatesTopic(
            @Value("${insights.orders.topic:order-progress-updates}") String orderUpdatesTopicName,
            @Value("${kafka.topic.retention.ms:86400000}") String retentionMs
    ) {
        return TopicBuilder.name(orderUpdatesTopicName).partitions(5).replicas(1).config("retention.ms", retentionMs).build();
    }

    @Bean
    public AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    @Bean
    public ConsumerFactory<String, FileProgressEvent> fileProgressConsumerFactory(
            @Value("${file.progress.group-id:file-progress-processor}") String groupId) {
         Map<String, Object> props = baseProps(groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class); // Use ErrorHandlingDeserializer
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class); // Specify the delegate deserializer
        JsonDeserializer<FileProgressEvent> deserializer = new JsonDeserializer<>(FileProgressEvent.class, new ObjectMapper(), false);
        deserializer.addTrustedPackages("com.salescode.dis.insights", "java.util");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new ErrorHandlingDeserializer<>(deserializer));
    }

    @Bean(name = "fileProgressContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, FileProgressEvent> fileProgressContainerFactory(
            ConsumerFactory<String, FileProgressEvent> fileProgressConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, FileProgressEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(fileProgressConsumerFactory);
        factory.setBatchListener(true);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, String> orderProgressConsumerFactory(
            @Value("${insights.order.group-id:order-progress-processor}") String groupId) {
        Map<String, Object> props = baseProps(groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer());
    }

    @Bean(name = "orderProgressContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> orderProgressContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(false);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, byte[]> orderDetailsConsumerFactory(
            @Value("${order.details.group-id:order-details-processor}") String groupId) {
        Map<String, Object> props = baseProps(groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new ByteArrayDeserializer());
    }

    @Bean(name = "orderDetailsContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> orderDetailsContainerFactory(
            ConsumerFactory<String, byte[]> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(false);
        return factory;
    }


}