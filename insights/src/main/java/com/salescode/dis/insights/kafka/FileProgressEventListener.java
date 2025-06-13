package com.salescode.dis.insights.kafka;

import com.salescode.dis.insights.dto.FileProgressRequest;
import com.salescode.dis.insights.service.FileService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@EnableKafka
@Slf4j
@RequiredArgsConstructor
@Profile("kafka")
public class FileProgressEventListener {

    private static final String FAILURE_TOPIC = "file-progress-updates-failed";

    private final FileService fileService;
    private final KafkaTemplate<String, FileProgressEvent> kafkaTemplate;

    /**
      In case of file, since it's already mapped to jobId, therefore in event jobId will be null.
      While on the other hand, In case of api, since we cannot register job beforehand, jobId should be sent inside event to create job if not there
     */
    @KafkaListener(topics = "${file.progress.update.topic:file-progress-updates}", groupId = "file-progress-processor", batch = "true", properties = {
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=100"
    })
    public void consumeProgressEvents(@Payload List<FileProgressEvent> events) {
        if (events == null || events.isEmpty()) {
            log.debug("Received empty or null event list. Skipping.");
            return;
        }
        log.info("Received {} events to process.", events.size());

        Map<String, AggregationWrapper> aggregationMap = new HashMap<>();

        events.forEach(event -> {
            try {
                if (event == null || event.getFileId() == null || event.getMasterName() == null) {
                    log.warn("Skipping invalid event (null or missing fileId or masterName): {}", event);
                    return;
                }

                String fileId = event.getFileId();
                String masterName = event.getMasterName();
                String key = fileId + ":" + masterName;

                FileProgressRequest progress = event.getProgress();

                if (progress != null) {
                    AggregationWrapper wrapper = aggregationMap.computeIfAbsent(key, k -> {
                        FileProgressEvent newAggregatedEvent = new FileProgressEvent();
                        FileProgressRequest newProgress = FileProgressRequest.createNewInstance();
                        newAggregatedEvent.setProgress(newProgress);
                        enrichEventMetadata(newAggregatedEvent, event);
                        return new AggregationWrapper(newAggregatedEvent, new ArrayList<>());
                    });

                    wrapper.addOriginalEvent(event);
                    aggregateProgress(wrapper.getAggregatedEvent(), event);

                } else {
                    log.warn("Skipping event for fileId {} due to null progress data: {}", fileId, event);
                }
            } catch (Exception e) {
                log.error("Failed to process individual event before aggregation: FileId={}, Event={}", (event != null ? event.getFileId() : "unknown"), event, e);
                sendToFailureTopic(event, "Individual event processing failed: " + e.getMessage());
            }
        });

        log.info("Aggregation complete. Processing {} aggregated updates.", aggregationMap.size());
        processAggregatedUpdates(aggregationMap);
    }

    private void aggregateProgress(FileProgressEvent aggregatedEvent, FileProgressEvent event) {
        if (aggregatedEvent == null || aggregatedEvent.getProgress() == null || event == null || event.getProgress() == null) {
            log.warn("Skipping aggregation due to null data. Aggregated: {}, Event: {}", aggregatedEvent, event);
            return;
        }

        FileProgressRequest existing = aggregatedEvent.getProgress();
        FileProgressRequest incoming = event.getProgress();

        // Aggregate successCount and failureCount
        existing.setSuccessCount(safeSum(existing.getSuccessCount(), incoming.getSuccessCount()));
        existing.setFailureCount(safeSum(existing.getFailureCount(), incoming.getFailureCount()));

        // Aggregate minProcessingTimeMs
        if (incoming.getMinProcessingTimeMs() != null) {
            existing.setMinProcessingTimeMs(existing.getMinProcessingTimeMs() == null ?
                    incoming.getMinProcessingTimeMs() : Math.min(existing.getMinProcessingTimeMs(), incoming.getMinProcessingTimeMs()));
        }
        // Aggregate maxProcessingTimeMs
        if (incoming.getMaxProcessingTimeMs() != null) {
            existing.setMaxProcessingTimeMs(existing.getMaxProcessingTimeMs() == null ?
                    incoming.getMaxProcessingTimeMs() : Math.max(existing.getMaxProcessingTimeMs(), incoming.getMaxProcessingTimeMs()));
        }

        // The stageName and modeOfIntegration should be consistent across aggregated events for the same file.
        // We will assume the first one encountered is the canonical one, or ideally, these should be verified
        // before aggregation if they can vary. For simplicity, we'll just set them if not already set.
        if (existing.getStageName() == null) {
            existing.setStageName(incoming.getStageName());
        }
    }

    private void enrichEventMetadata(FileProgressEvent aggregatedEvent, FileProgressEvent event) {
        if (aggregatedEvent == null || event == null) {
            log.warn("Skipping metadata enrichment due to null event(s). Aggregated: {}, Event: {}", aggregatedEvent, event);
            return;
        }
        aggregatedEvent.setFileId(event.getFileId());
        // Keep first non-null value encountered for metadata fields
        if (aggregatedEvent.getLob() == null) aggregatedEvent.setLob(event.getLob());
        if (aggregatedEvent.getJobId() == null) aggregatedEvent.setJobId(event.getJobId());
        if (aggregatedEvent.getMasterName() == null) aggregatedEvent.setMasterName(event.getMasterName());
        aggregatedEvent.setErrorMessage(null); // Reset error message on aggregated event
    }

    private void processAggregatedUpdates(Map<String, AggregationWrapper> aggregationMap) {
        aggregationMap.forEach((key, wrapper) -> {
            if (wrapper == null) {
                log.error("Encountered null wrapper in aggregationMap for key: {}. Skipping.", key);
                return;
            }

            FileProgressEvent aggregatedEvent = wrapper.getAggregatedEvent();
            List<FileProgressEvent> originalEvents = wrapper.getOriginalEventsInBatch();

            try {
                if (aggregatedEvent == null || aggregatedEvent.getProgress() == null) {
                    log.error("Skipping update for key {} due to null aggregated event or progress. Sending original events to failure topic.", key);
                    sendOriginalsToFailureTopic(originalEvents, "Aggregated event or progress was null", key);
                    return;
                }

                fileService.updateProgress(
                        aggregatedEvent.getFileId(),
                        aggregatedEvent.getMasterName(),
                        aggregatedEvent.getProgress()
                );
                log.debug("Progress updated successfully for key: {}", key);

            } catch (Exception e) {
                log.error("Failed to update aggregated progress for key: {}. Sending original events to failure topic.", key, e);
                sendOriginalsToFailureTopic(originalEvents, "Aggregated update failed: " + e.getMessage(), key);
            }
        });
    }

    private void sendOriginalsToFailureTopic(List<FileProgressEvent> originalEvents, String errorMessage, String key) {
        if (originalEvents == null || originalEvents.isEmpty()) {
            log.warn("No original events to send to failure topic for key: {} due to error: {}", key, errorMessage);
            return;
        }

        log.info("Sending {} original events to failure topic for key {} due to error: {}", originalEvents.size(), key, errorMessage);
        originalEvents.forEach(event -> sendToFailureTopic(event, errorMessage));
    }

    private void sendToFailureTopic(FileProgressEvent eventToSend, String errorMessage) {
        if (eventToSend == null) {
            log.error("Cannot send null event to failure topic. Error message was: {}", errorMessage);
            return;
        }

        try {
            String finalErrorMessage = errorMessage;
            if (eventToSend.getErrorMessage() != null && !eventToSend.getErrorMessage().isEmpty()) {
                finalErrorMessage = eventToSend.getErrorMessage() + " | Additionally: " + errorMessage;
            }
            eventToSend.setErrorMessage(finalErrorMessage);

            String key = eventToSend.getFileId() != null ? eventToSend.getFileId() : "unknows";
            kafkaTemplate.send(FAILURE_TOPIC, key, eventToSend);
            log.info("Sent event to topic '{}' with key '{}'. Reason: {}", FAILURE_TOPIC, key, finalErrorMessage);

        } catch (Exception sendEx) {
            log.error("CRITICAL: Failed to send event to failure topic '{}' for FileId: {}. Original Error: {}. Send Error:",
                    FAILURE_TOPIC, eventToSend.getFileId(), errorMessage, sendEx);
        }
    }

    private Long safeSum(Long a, Long b) {
        return Optional.ofNullable(a).orElse(0L) + Optional.ofNullable(b).orElse(0L);
    }

    @Getter
    @AllArgsConstructor
    private static class AggregationWrapper {
        private final FileProgressEvent aggregatedEvent;
        private final List<FileProgressEvent> originalEventsInBatch;

        public void addOriginalEvent(FileProgressEvent event) {
            this.originalEventsInBatch.add(event);
        }
    }
}
