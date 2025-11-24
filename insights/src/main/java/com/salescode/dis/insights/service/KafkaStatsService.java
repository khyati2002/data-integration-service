package com.salescode.dis.insights.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.salescode.dis.insights.dto.TopicStats;
import com.salescode.dis.insights.dto.TopicStatsResponse;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Caffeine;

@Service
@Profile("kafka")
public class KafkaStatsService {

    private final AdminClient kafkaAdminClient;
    private final PropertyService propertyService;
    private final Cache<String, TopicStatsResponse> metricsCache;

    @Autowired
    public KafkaStatsService(AdminClient kafkaAdminClient,
                             PropertyService propertyService,
                             @Value("${spring.kafka.metrics.cache-expiry-seconds:5}") long cacheExpirySeconds
    ) {
        this.kafkaAdminClient = kafkaAdminClient;
        this.propertyService = propertyService;
        this.metricsCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheExpirySeconds, TimeUnit.SECONDS)
                .maximumSize(1000)
                .build();
    }
    public TopicStatsResponse getTopicsStats(String consumerGroup, String env) {

        String cacheKey = env + ":" + consumerGroup;

        TopicStatsResponse cached = metricsCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<TopicStats> lobStats = computeTopicStats(consumerGroup, env);

        TopicStatsResponse response = new TopicStatsResponse(Instant.now(), lobStats);

        metricsCache.put(cacheKey, response);

        return response;
    }

    private List<TopicStats> computeTopicStats(String consumerGroup, String env) {
        List<String> lobNames = propertyService.getLobsForEnv(env);

        Set<String> existingTopics;
        try {
            existingTopics = kafkaAdminClient.listTopics().names().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to list topics from Kafka", e);
        }

        Map<org.apache.kafka.common.TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> committedOffsets;
        try {
            committedOffsets = kafkaAdminClient
                    .listConsumerGroupOffsets(consumerGroup)
                    .partitionsToOffsetAndMetadata()
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to fetch consumer group offsets", e);
        }

        Map<String, List<org.apache.kafka.common.TopicPartition>> topicPartitionsMap = new HashMap<>();
        for (String lob : lobNames) {
            String topic = lob + "-integration-streams";
            if (!existingTopics.contains(topic)) continue;

            List<org.apache.kafka.common.TopicPartition> partitions = committedOffsets.keySet().stream()
                    .filter(tp -> tp.topic().equals(topic))
                    .toList();

            if (!partitions.isEmpty()) {
                topicPartitionsMap.put(topic, partitions);
            }
        }

        Map<org.apache.kafka.common.TopicPartition, org.apache.kafka.clients.admin.OffsetSpec> offsetSpecMap = new HashMap<>();
        for (List<org.apache.kafka.common.TopicPartition> partitions : topicPartitionsMap.values()) {
            for (org.apache.kafka.common.TopicPartition tp : partitions) {
                offsetSpecMap.put(tp, org.apache.kafka.clients.admin.OffsetSpec.latest());
            }
        }

        Map<org.apache.kafka.common.TopicPartition, org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo> latestOffsets;
        try {
            latestOffsets = kafkaAdminClient.listOffsets(offsetSpecMap).all().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException("Failed to fetch latest offsets", ex);
        }

        List<TopicStats> result = new ArrayList<>();

        for (Map.Entry<String, List<org.apache.kafka.common.TopicPartition>> entry : topicPartitionsMap.entrySet()) {
            String topic = entry.getKey();
            List<org.apache.kafka.common.TopicPartition> partitions = entry.getValue();

            long committedTotal = 0L;
            long latestTotal = 0L;

            for (org.apache.kafka.common.TopicPartition tp : partitions) {
                long committed = committedOffsets.get(tp).offset();
                long latest = latestOffsets.get(tp).offset();
                committedTotal += committed;
                latestTotal += latest;
            }

            long pending = latestTotal - committedTotal;

            String lob = topic.replace("-integration-streams", "");

            TopicStats stats = new TopicStats();
            stats.setLob(lob);
            stats.setCommittedOffset(committedTotal);
            stats.setLatestOffset(latestTotal);
            stats.setPending(pending);
            result.add(stats);
        }

        return result;
    }

}