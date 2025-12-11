package com.salescode.dim;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Slf4j
public class KafkaTopicCreator {

    // ConcurrentSet to store the checked topics
    private static final Set<String> topicCache = ConcurrentHashMap.newKeySet();

    public static void createTopicIfNotExists(String topic, String bootstrapServers, int partitions, short replicationFactor) {
        // First check if the topic exists in the cache
        if (topic != null && topicCache.contains(topic)) {
            log.debug("Topic {} already exists", topic);
            return;
        }

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(props)) {
            // Check if the topic already exists
            if (!adminClient.listTopics().names().get().contains(topic)) {
                NewTopic newTopic = new NewTopic(topic, partitions, replicationFactor);
                adminClient.createTopics(Collections.singleton(newTopic)).all().get();
                topicCache.add(topic);  // Add to cache after creating the topic
                log.info("Topic '{}' created successfully.", topic);
            } else {
                topicCache.add(topic);  // Add to cache if topic exists
                log.info("Topic '{}' already exists.", topic);
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Error creating Kafka topic", e);
        }
    }

    public static void createTopicIfNotExists(String topic, String bootstrapServers) {
        createTopicIfNotExists(topic, bootstrapServers, 5, (short) 1);
    }

    // Method to clear the topic
    private static void clearTopic(String topic, String bootstrapServers) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(props)) {
            // Delete the topic if it exists
            adminClient.deleteTopics(Collections.singleton(topic)).all().get();
            topicCache.remove(topic); // Remove topic from cache
            log.info("Topic '{}' cleared (deleted).", topic);
        } catch (ExecutionException | InterruptedException e) {
            if(e.getCause() != null && e.getCause() instanceof UnknownTopicOrPartitionException){
                log.error("Error cleared Kafka topic", e);
            } else throw new RuntimeException("Error deleting Kafka topic", e);
        }
    }


    public static void clearAndRecreateTopic(String topic, String bootstrapServers) {
        // First delete the topic if it exists
        clearTopic(topic, bootstrapServers);

        // Now create the topic again
        createTopicIfNotExists(topic, bootstrapServers);
    }
}