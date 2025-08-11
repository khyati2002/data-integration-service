package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.TopicStats;
import com.salescode.dis.insights.service.KafkaStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("")
public class StatisticsController {

    private final KafkaStatsService kafkaService;
    public static final String INTEGRATION_GROUP_ID_CONFIG = "consumerGroupIntegrations";

    @Autowired
    public StatisticsController(KafkaStatsService kafkaService) {
        this.kafkaService = kafkaService;
    }

    @GetMapping("/integration-stats")
    public ResponseEntity<List<TopicStats>> getStatsByEnv(
            @RequestParam("env") String env
    ) {

        String consumerName = ("prod".equalsIgnoreCase(env)||"prod-egtm".equalsIgnoreCase(env))?INTEGRATION_GROUP_ID_CONFIG:env+"-"+INTEGRATION_GROUP_ID_CONFIG;

        List<TopicStats> kafkaTopicMetrics =
                kafkaService.getTopicsStats(consumerName, env);

        return ResponseEntity.ok(kafkaTopicMetrics);
    }
}
