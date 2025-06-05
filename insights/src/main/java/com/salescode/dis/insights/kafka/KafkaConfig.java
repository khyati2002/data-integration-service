package com.salescode.dis.insights.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("kafka")
public class KafkaConfig {

    @Bean
    public NewTopic fileUpdatesTopic(@Value("${file.progress.update.topic:file-progress-updates}") String fileUpdatesTopicName) {
        // Creates the topic if it doesn't exist, with 5 partitions and replication factor of 1
        return TopicBuilder.name(fileUpdatesTopicName)
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