package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.TopicStatsResponse;
import com.salescode.dis.insights.service.KafkaStatsService;
import com.salescode.dis.insights.sse.SSEService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class StatisticsController {

    private final KafkaStatsService kafkaService;
    public static final String INTEGRATION_GROUP_ID_CONFIG = "consumerGroupIntegrations";
    private final SSEService sseService;

    @Autowired
    public StatisticsController(KafkaStatsService kafkaService, SSEService sseService) {
        this.kafkaService = kafkaService;
        this.sseService = sseService;
    }

    @GetMapping("/integration-stats")
    public SseEmitter streamStats(@RequestParam("env") String env) {
        String consumerName = ("prod".equalsIgnoreCase(env)||"prod-egtm".equalsIgnoreCase(env))
                ? INTEGRATION_GROUP_ID_CONFIG
                : env + "-" + INTEGRATION_GROUP_ID_CONFIG;

        String key = env + ":" + consumerName;
        long timeoutMillis = 30 * 60 * 1000L;
        SseEmitter emitter = sseService.addStatsEmitter(key, timeoutMillis);

        TopicStatsResponse initial = kafkaService.getTopicsStats(consumerName, env);
        try {
            emitter.send(SseEmitter.event()
                    .name("stats-update")
                    .data(initial, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            sseService.removeStatsEmitter(key, emitter);
        }

        return emitter;
    }
}
