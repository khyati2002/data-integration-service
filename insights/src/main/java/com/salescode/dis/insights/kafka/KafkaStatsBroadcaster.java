package com.salescode.dis.insights.kafka;

import com.salescode.dis.insights.dto.TopicStatsResponse;
import com.salescode.dis.insights.service.KafkaStatsService;
import com.salescode.dis.insights.service.SseService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KafkaStatsBroadcaster {

    private final KafkaStatsService kafkaStatsService;
    private final SseService sseService;

    private final Map<String, Integer> lastHash = new ConcurrentHashMap<>();

    public KafkaStatsBroadcaster(KafkaStatsService kafkaStatsService, SseService sseService) {
        this.kafkaStatsService = kafkaStatsService;
        this.sseService = sseService;
    }

    @Scheduled(fixedRateString = "${spring.kafka.metrics.broadcast-interval:6000}")
    public void broadcastAll() {
        for (String key : sseService.getActiveKeys()) {
            String[] parts = key.split(":", 2);
            if (parts.length < 2) continue;

            String env = parts[0];
            String consumerName = parts[1];

            TopicStatsResponse stats = kafkaStatsService.getTopicsStats(consumerName, env);

            int hash = stats.hashCode();
            if (hash == lastHash.getOrDefault(key, 0)) {
                continue;
            }
            lastHash.put(key, hash);
            sseService.broadcast(key, stats, "stats-update");
        }
    }
}
