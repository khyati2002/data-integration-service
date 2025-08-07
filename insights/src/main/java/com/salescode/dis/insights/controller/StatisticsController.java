package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.TopicStats;
import com.salescode.dis.insights.service.KafkaStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatisticsController {

    private final KafkaStatsService kafkaService;

    @Autowired
    public StatisticsController(KafkaStatsService kafkaService) {
        this.kafkaService = kafkaService;
    }

    @GetMapping("/integration-stats")
    public ResponseEntity<List<TopicStats>> getStatsByEnv(
            @RequestParam("env") String env
    ) {
        final String consumerGroup = "uat-consumerGroupIntegrations";

        List<TopicStats> kafkaTopicMetrics =
                kafkaService.getTopicsStats(consumerGroup, env);

        return ResponseEntity.ok(kafkaTopicMetrics);
    }
}
