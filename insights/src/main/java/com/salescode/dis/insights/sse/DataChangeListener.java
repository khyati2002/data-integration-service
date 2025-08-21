package com.salescode.dis.insights.sse;

import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.entity.StageMetadata;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class DataChangeListener {

    private final ApplicationEventPublisher eventPublisher;

    private static final ThreadLocal<Set<String>> PROCESSING_ENTITIES =
            ThreadLocal.withInitial(ConcurrentHashMap::newKeySet);

    @PostPersist
    @PostUpdate
    @PostRemove
    public void handleDataChange(Object entity) {
        String entityKey = generateEntityKey(entity);
        // Prevent recursive calls for the same entity
        if (PROCESSING_ENTITIES.get().contains(entityKey)) {
            log.debug("Skipping recursive data change event for: {}", entityKey);
            return;
        }
        log.debug("Data change detected for entity: {}", entity.getClass().getSimpleName());
        String eventType = determineEventType(entity);
        if (eventType != null) {
            String lob = extractLobFromEntity(entity);
            DataChangeEvent event = new DataChangeEvent(eventType, entity, lob);
            DataChangeEvent immediateEvent = new DataChangeEvent(eventType + "_IMMEDIATE", entity, lob);
            eventPublisher.publishEvent(immediateEvent);
            // Delayed event for final consistency (fires after commit)
            eventPublisher.publishEvent(event);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Async
    public void handleImmediateDataChange(DataChangeEvent event) {
        if (!event.getEventType().endsWith("_IMMEDIATE")) {
            return;
        }
        String entityKey = generateEntityKey(event.getEntity());
        PROCESSING_ENTITIES.get().add(entityKey);

        try {
            log.info("Handling immediate data change event: {}", event.getEventType());

            SSEService sseService = ApplicationContextHolder.getContext().getBean(SSEService.class);
            String baseEventType = event.getEventType().replace("_IMMEDIATE", "");

            handleEventByType(baseEventType, event, sseService, true);

        } catch (Exception e) {
            log.error("Error handling immediate data change event: {}", event.getEventType(), e);
        } finally {
            PROCESSING_ENTITIES.get().remove(entityKey);
        }
    }

    // Handle final events (after transaction commit)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleAsyncDataChange(DataChangeEvent event) {
        if (event.getEventType().endsWith("_IMMEDIATE")) {
            return;
        }

        String entityKey = generateEntityKey(event.getEntity());
        PROCESSING_ENTITIES.get().add(entityKey);

        try {
            log.info("Handling async data change event: {}", event.getEventType());

            SSEService sseService = ApplicationContextHolder.getContext().getBean(SSEService.class);

            handleEventByType(event.getEventType(), event, sseService, false);

        } catch (Exception e) {
            log.error("Error handling async data change event: {}", event.getEventType(), e);
        } finally {
            PROCESSING_ENTITIES.get().remove(entityKey);
        }
    }

    private void handleEventByType(String eventType, DataChangeEvent event, SSEService sseService, boolean isImmediate) {
        switch (eventType) {
            case "JOB_UPDATE":
                handleAllJobDataChange(event, sseService, isImmediate);
                break;
            case "SUMMARY_UPDATE":
                handleLobSummaryDataChange(event, sseService, isImmediate);
                handleAllJobDataChange(event, sseService, isImmediate);
                handleFileUpdate(event, sseService);
                break;
            case "STAGE_UPDATE":
                handleStageDataChange(sseService);
                break;
            case "FILE_UPDATE":
                handleFileUpdate(event, sseService);
                break;
        }
    }

    private String generateEntityKey(Object entity) {
        String className = entity.getClass().getSimpleName();
        String id = extractEntityId(entity);
        return className + "_" + (id != null ? id : entity.hashCode());
    }

    private String extractEntityId(Object entity) {
        if (entity instanceof JobEntity job) return job.getId();
        if (entity instanceof FileEntity file) return file.getFileId();
        if (entity instanceof FileStageMetrics metrics) return String.valueOf(metrics.getId());
        if (entity instanceof StageMetadata stage) return String.valueOf(stage.getId());
        return null;
    }

    private String determineEventType(Object entity) {
        if (entity instanceof JobEntity) return "JOB_UPDATE";
        if (entity instanceof FileEntity) return "FILE_UPDATE";
        if (entity instanceof FileStageMetrics) return "SUMMARY_UPDATE";
        if (entity instanceof StageMetadata) return "STAGE_UPDATE";
        return null;
    }

    private void handleAllJobDataChange(DataChangeEvent event, SSEService sseService, boolean isImmediate) {
        try {
            String lob = event.getLobId();
            String jobId = extractJobIdFromEntity(event.getEntity());
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = isImmediate ? endDate.minusHours(1) : endDate.minusDays(10);
            sseService.broadcastAllJobsUpdate(lob, null, startDate, endDate, jobId);
        } catch (Exception e) {
            log.error("Error handling job data change", e);
        }
    }

    private void handleLobSummaryDataChange(DataChangeEvent event, SSEService sseService, boolean isImmediate) {
        try {
            String lob = event.getLobId();
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = isImmediate ? endDate.minusHours(1) : endDate.minusDays(10);

            sseService.broadcastLobSummaryUpdate(lob, startDate, endDate);

        } catch (Exception e) {
            log.error("Error handling summary data change", e);
        }
    }

    private void handleStageDataChange(SSEService sseService) {
        try {
            sseService.broadcastStagesData();
        } catch (Exception e) {
            log.error("Error handling stage data change", e);
        }
    }

    private void handleFileUpdate(DataChangeEvent event, SSEService sseService) {
        try {
            String lob = event.getLobId();
            String jobId = extractJobIdFromEntity(event.getEntity());
            String masterName = extractMasterName(event.getEntity());
            String fileId = extractFileId(event.getEntity());

            sseService.broadcastFileDetailUpdate(lob, masterName, jobId, fileId);

        } catch (Exception e) {
            log.error("Error handling file progress update", e);
        }
    }

    private String extractLobFromEntity(Object entity) {
        if (entity instanceof JobEntity job) return job.getLob();
        if (entity instanceof FileEntity file) return file.getJob() != null ? file.getJob().getLob() : null;
        if (entity instanceof FileStageMetrics metrics) return metrics.getJob() != null ? metrics.getJob().getLob() : null;
        log.warn("extractLobFromEntity: Unsupported entity type {}", entity.getClass().getName());
        return null;
    }

    private String extractJobIdFromEntity(Object entity) {
        if (entity instanceof JobEntity job) return job.getId();
        if (entity instanceof FileEntity file) return file.getJob() != null ? file.getJob().getId() : null;
        if (entity instanceof FileStageMetrics metrics) return metrics.getJob() != null ? metrics.getJob().getId() : null;
        log.warn("extractJobIdFromEntity: Unsupported entity type {}", entity.getClass().getName());
        return null;
    }

    private String extractFileId(Object entity) {
        if (entity instanceof FileEntity file) return file.getFileId();
        if (entity instanceof FileStageMetrics metrics) return metrics.getFile().getFileId();
        return null;
    }

    private String extractMasterName(Object entity) {
        if (entity instanceof FileEntity file)   return  file.getMaster();
        if (entity instanceof FileStageMetrics metrics) return metrics.getFile().getMaster();
        return null;
    }

    @PreDestroy
    public void cleanup() {
        PROCESSING_ENTITIES.remove();
    }

    @Getter
    public static class DataChangeEvent {
        private final String eventType;
        private final Object entity;
        private final String lobId;
        private final LocalDateTime timestamp;

        public DataChangeEvent(String eventType, Object entity, String lobId) {
            this.eventType = eventType;
            this.entity = entity;
            this.lobId = lobId;
            this.timestamp = LocalDateTime.now();
        }
    }
}