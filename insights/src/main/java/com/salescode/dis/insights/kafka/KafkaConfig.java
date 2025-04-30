package com.salescode.dis.insights.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

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
//
//    @Bean
//    public ConsumerFactory<String, FileUpdateEvent> fileUpdateConsumerFactory() {
//        JsonDeserializer<FileUpdateEvent> deserializer = new JsonDeserializer<>(FileUpdateEvent.class);
//        deserializer.setRemoveTypeHeaders(false);
//        deserializer.setUseTypeMapperForKey(true);
//        deserializer.addTrustedPackages("com.salescode.dis.insights.kafka");
//
//        return new DefaultKafkaConsumerFactory<>(
//                Map.of(
//                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
//                        ConsumerConfig.GROUP_ID_CONFIG, "file-update-processor",
//                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
//                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class
//                ),
//                new StringDeserializer(),
//                deserializer
//        );
//    }
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, FileUpdateEvent> kafkaListenerContainerFactory() {
//        ConcurrentKafkaListenerContainerFactory<String, FileUpdateEvent> factory =
//                new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(fileUpdateConsumerFactory());
//        factory.setBatchListener(true); // Enable batch processing
//        return factory;
//    }


}