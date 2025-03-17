package com.salescode.dim;

import org.apache.kafka.clients.admin.*;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class KafkaTopicCreator {

    public static void createTopicIfNotExists(String topic, String bootstrapServers, int partitions, short replicationFactor) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(props)) {
            // Check if topic already exists
            if (!adminClient.listTopics().names().get().contains(topic)) {
                NewTopic newTopic = new NewTopic(topic, partitions, replicationFactor);
                adminClient.createTopics(Collections.singleton(newTopic)).all().get();
                System.out.println("Topic '" + topic + "' created successfully.");
            } else {
                System.out.println("Topic '" + topic + "' already exists.");
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Error creating Kafka topic", e);
        }
    }
}