package com.salescode.dis.insights.kafka;

import com.salescode.dis.insights.dto.event.FileProgressEvent;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.event.ObservabilityEventProducer;
import com.salescode.dis.insights.service.FileService;
import com.salescode.dis.insights.validation.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@EnableKafka
@Slf4j
@RequiredArgsConstructor
@Profile("kafka")
public class FileProgressEventListener {

    @Value("${file.progress.update.failure.topic:file-progress-updates-failed}")
    private String FAILURE_TOPIC;

    private final FileService fileService;
    private final KafkaTemplate<String, FileProgressEvent> kafkaTemplate;
    private final ValidationService validationService;
    private final ObservabilityEventProducer eventProducer;

    @KafkaListener(topics = "${file.progress.update.topic:file-progress-updates-1}",
            groupId = "file-progress-processor", batch = "true",
            containerFactory = "fileProgressContainerFactory",
            properties = {
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "=10000"
    })
    public void consumeProgressEvents(@Payload List<FileProgressEvent> events) throws InterruptedException {
        if (events == null || events.isEmpty()) {
            log.debug("Received empty or null event list. Skipping.");
            return;
        }

        log.info("Received {} events to process.", events.size());
        Map<String, AggregationWrapper> aggregationMap = aggregate(events);
        processAggregatedUpdates(aggregationMap);
        Map<String,AggregationWrapper> LobAndMasterAggregation =  aggregateByLobAndMaster(events);
        sendEvents(LobAndMasterAggregation);
    }

    private Map<String, AggregationWrapper> aggregate(List<FileProgressEvent> events) {
        Map<String, AggregationWrapper> aggregationMap = new HashMap<>();

        for (FileProgressEvent event : events) {
            Optional<String> validationError = validate(event);
            if (validationError.isPresent()) {
                String errorMsg = validationError.get();
                log.warn("Invalid event: {}. Reason: {}", event, errorMsg);
                sendToFailureTopic(event, errorMsg);
                continue;
            }

            String key = buildKey(event);
            aggregationMap.computeIfAbsent(key, k -> new AggregationWrapper(event)).addEvent(event);
        }
        return aggregationMap;
    }

    private void processAggregatedUpdates(Map<String, AggregationWrapper> aggregationMap) {
        aggregationMap.forEach((key, wrapper) -> {
            FileProgressEvent aggregatedEvent = wrapper.getAggregatedEvent();
            try {
                fileService.updateProgress(aggregatedEvent.getFileId(), aggregatedEvent.getMasterName(), aggregatedEvent.getJobId(), aggregatedEvent.getLob(), aggregatedEvent.getProgress());
                log.debug("Progress updated successfully for key: {}", key);
            } catch (Exception e) {
                log.error("Failed to update aggregated progress for key: {}", key, e);
                wrapper.getOriginalEvents().forEach(event -> sendToFailureTopic(event, e.getMessage()));
            }
        });
    }

    private Optional<String> validate(FileProgressEvent event) {
        if (event == null) return Optional.of("Event is null");
        if (event.getFileId() == null) return Optional.of("FileId is null");
        if (event.getMasterName() == null) return Optional.of("MasterName is null");
        if (event.getProgress() == null) return Optional.of("Progress is null");
        if (event.getProgress().getStageType() == null) return Optional.of("StageName is null");
        return Optional.empty();
    }

    private String buildKey(FileProgressEvent event) {
        return event.getFileId() + ":" + event.getMasterName() + ":" + event.getProgress().getStageType();
    }

    private void sendToFailureTopic(FileProgressEvent event, String errorMessage) {
        if (event == null) return;
        try {
            event.setErrorMessage(Optional.ofNullable(event.getErrorMessage())
                    .map(existing -> existing + " | " + errorMessage)
                    .orElse(errorMessage));
            kafkaTemplate.send(FAILURE_TOPIC, Optional.ofNullable(event.getFileId()).orElse("unknown"), event);
            log.info("Sent event to failure topic.");
        } catch (Exception ex) {
            log.error("CRITICAL: Failed to send to failure topic", ex);
        }
    }

    private void sendEvents(Map<String, AggregationWrapper> aggregationMap){
        aggregationMap.forEach((key, wrapper) -> {
            FileProgressEvent aggregatedEvent = wrapper.getAggregatedEvent();
            try {
                eventProducer.emitProgressAggregatedEvent(aggregatedEvent);
            } catch (Exception e) {
                log.error("Failed to update aggregated progress for key: {}", key, e);
            }
        });
    }

    private Map<String,AggregationWrapper> aggregateByLobAndMaster(List<FileProgressEvent> events) {
        Map<String, AggregationWrapper> aggregationMap = new HashMap<>();

        for (FileProgressEvent event : events) {
            Optional<String> validationError = validate(event);
            if (validationError.isPresent()) {
                String errorMsg = validationError.get();
                log.warn("Invalid event: {}. Reason: {}", event, errorMsg);
                continue;
            }
            String key = buildLobMasterKey(event);
            aggregationMap.computeIfAbsent(key, k -> new AggregationWrapper(event)).addEvent(event);
        }
        return aggregationMap;
    }

    private String buildLobMasterKey(FileProgressEvent event) {
        String lob = Optional.ofNullable(event.getLob()).orElse("unknown");
        String masterName = Optional.ofNullable(event.getMasterName()).orElse("unknown");
        String stageName = Optional.ofNullable(event.getProgress())
                .map(p -> p.getStageType())
                .map(Enum::name)
                .orElse("UNKNOWN");
        return lob + ":" + masterName + ":" + stageName;
    }
}