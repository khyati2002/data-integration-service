package com.salescode.dis.insights.kafka;

import com.salescode.dis.insights.dto.FileProgressRequest;
import com.salescode.dis.insights.service.FileService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@EnableKafka
@Slf4j
@RequiredArgsConstructor
public class FileProgressEventListener {

    private static final String FAILURE_TOPIC = "file-progress-updates-failed"; // Define failure topic name

    private final FileService fileService;
    private final KafkaTemplate<String, FileProgressEvent> kafkaTemplate;

    /*
      In case of file, since it's already mapped to jobId, therefore in event jobId will be null.
      While on the other hand, In case of api, since we cannot register job beforehand, jobId should be sent inside event to create job if not there
     */
    @KafkaListener(topics = "file-progress-updates", groupId = "file-progress-processor", batch = "true", properties = {
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=100"
    })
    public void consumeProgressEvents(List<FileProgressEvent> events) {
        if (events == null || events.isEmpty()) {
            log.debug("Received empty or null event list. Skipping.");
            return;
        }
        log.info("Received {} events to process.", events.size());

        Map<String, AggregationWrapper> aggregationMap = new HashMap<>();

        events.forEach(event -> {
            try {
                if (event == null || event.getFileId() == null) {
                    log.warn("Skipping invalid event (null or missing fileId): {}", event);
                    return;
                }

                String fileId = event.getFileId();
                FileProgressRequest progress = event.getProgress();

                if (progress != null) {
                    AggregationWrapper wrapper = aggregationMap.computeIfAbsent(fileId, k -> {
                        FileProgressEvent newAggregatedEvent = new FileProgressEvent();
                        FileProgressRequest newProgress = FileProgressRequest.createNewInstance();
                        newAggregatedEvent.setProgress(newProgress);
                        enrichEventMetadata(newAggregatedEvent, event);
                        return new AggregationWrapper(newAggregatedEvent, new ArrayList<>());
                    });

                    // Add the current original event to the wrapper's list
                    wrapper.addOriginalEvent(event);

                    // Aggregate progress into the wrapper's aggregatedEvent
                    aggregateProgress(wrapper.getAggregatedEvent(), event);

                } else {
                    log.warn("Skipping event for fileId {} due to null progress data: {}", fileId, event);
                }

            } catch (Exception e) {
                // Catch any exception during individual event processing/aggregation
                log.error("Failed to process individual event before aggregation: FileId={}, Event={}", (event != null ? event.getFileId() : "unknown"), event, e);
                // Send the original failed event to the failure topic
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

        if (existing.getConsumer() == null) existing.setConsumer(new FileProgressRequest.ConsumerMetrics());
        if (existing.getPublisher() == null) existing.setPublisher(new FileProgressRequest.PublisherMetrics());

        // Aggregate if incoming metrics are present
        if (incoming.getConsumer() != null) {
            aggregateMetrics(existing.getConsumer(), incoming.getConsumer());
        }
        if (incoming.getPublisher() != null) {
            aggregateMetrics(existing.getPublisher(), incoming.getPublisher());
        }
    }

    private void aggregateMetrics(FileProgressRequest.ConsumerMetrics existing, FileProgressRequest.ConsumerMetrics incoming) {
        if (existing == null || incoming == null) {
            log.warn("Skipping consumer metrics aggregation due to null object(s). Existing: {}, Incoming: {}", existing, incoming);
            return;
        }
        existing.setSuccessCount(safeSum(existing.getSuccessCount(), incoming.getSuccessCount()));
        existing.setServerFailCount(safeSum(existing.getServerFailCount(), incoming.getServerFailCount()));
        existing.setLogicalFailCount(safeSum(existing.getLogicalFailCount(), incoming.getLogicalFailCount()));
    }

    private void aggregateMetrics(FileProgressRequest.PublisherMetrics existing, FileProgressRequest.PublisherMetrics incoming) {
        if (existing == null || incoming == null) {
            log.warn("Skipping publisher metrics aggregation due to null object(s). Existing: {}, Incoming: {}", existing, incoming);
            return;
        }
        existing.setSuccessCount(safeSum(existing.getSuccessCount(), incoming.getSuccessCount()));
        existing.setFailCount(safeSum(existing.getFailCount(), incoming.getFailCount()));
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

    // Modified to accept the map of wrappers and handle failures by sending original events
    private void processAggregatedUpdates(Map<String, AggregationWrapper> aggregationMap) {
        aggregationMap.forEach((fileId, wrapper) -> {
            if (wrapper == null) {
                log.error("Encountered null wrapper in aggregationMap for fileId: {}. Skipping.", fileId);
                return; // Skip this entry
            }
            FileProgressEvent aggregatedEvent = wrapper.getAggregatedEvent();
            List<FileProgressEvent> originalEvents = wrapper.getOriginalEventsInBatch();

            try {
                if (aggregatedEvent == null || aggregatedEvent.getProgress() == null) {
                    log.error("Skipping update for fileId {} due to null aggregated event or progress within wrapper. Sending original events to failure topic.", fileId);
                    sendOriginalsToFailureTopic(originalEvents, "Aggregated event or progress was null before final update", fileId);
                    return;
                }

                fileService.updateProgress(fileId, aggregatedEvent.getProgress(), aggregatedEvent.getJobId(), aggregatedEvent.getLob(), aggregatedEvent.getMasterName());
                log.debug("Progress updated successfully for file: {}", fileId);

            } catch (Exception e) {
                log.error("Failed to update aggregated progress for file: {}. Sending original events to failure topic.", fileId, e);
                sendOriginalsToFailureTopic(originalEvents, "Aggregated update failed: " + e.getMessage(), fileId);
            }
        });
    }

    // Helper method to send a list of original events to the failure topic
    private void sendOriginalsToFailureTopic(List<FileProgressEvent> originalEvents, String errorMessage, String fileIdForKey) {
        if (originalEvents == null || originalEvents.isEmpty()) {
            log.warn("No original events provided to send to failure topic for fileId: {} due to error: {}", fileIdForKey, errorMessage);
            return;
        }
        log.info("Sending {} original events to failure topic for fileId {} due to error: {}", originalEvents.size(), fileIdForKey, errorMessage);
        originalEvents.forEach(originalEvent -> {
            sendToFailureTopic(originalEvent, errorMessage);
        });
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
            log.error("CRITICAL: Failed to send event to failure topic '{}' for FileId: {}. Original Error: {}. Send Error:", FAILURE_TOPIC, eventToSend.getFileId(), errorMessage, sendEx);
        }
    }

    private Integer safeSum(Integer a, Integer b) {
        int valA = Optional.ofNullable(a).orElse(0);
        int valB = Optional.ofNullable(b).orElse(0);
        return valA + valB;
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