package com.salescode.dis.insights.sse;

import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.entity.StageMetadata;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.context.ApplicationContext;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class DataChangeListener {

    private final ApplicationEventPublisher eventPublisher;
    @Autowired
    ApplicationContext applicationContext;

    private static final ThreadLocal<Set<String>> PROCESSING_ENTITIES =
            ThreadLocal.withInitial(ConcurrentHashMap::newKeySet);

    @PostPersist
    @PostUpdate
    @PostRemove
    public void handleDataChange(Object entity) {
        String entityKey = generateEntityKey(entity);
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

            SSEService sseService = applicationContext.getBean(SSEService.class);
            String baseEventType = event.getEventType().replace("_IMMEDIATE", "");

            handleEventByType(baseEventType, event, sseService);

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

            SSEService sseService = applicationContext.getBean(SSEService.class);

            handleEventByType(event.getEventType(), event, sseService);

        } catch (Exception e) {
            log.error("Error handling async data change event: {}", event.getEventType(), e);
        } finally {
            PROCESSING_ENTITIES.get().remove(entityKey);
        }
    }

    private void handleEventByType(String eventType, DataChangeEvent event, SSEService sseService) {
        switch (eventType) {
            case "JOB_UPDATE":
                handleJobDataChange(event, sseService);
                break;
            case "FILE_UPDATE":
                handleFileUpdate(event, sseService);
                break;
            case "SUMMARY_UPDATE":
            default:
                handleJobDataChange(event, sseService);
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
        return null;
    }

    private void handleJobDataChange(DataChangeEvent event, SSEService sseService) {
        try {
            String lob = event.getLobId();
            String jobId = extractJobIdFromEntity(event.getEntity());
            sseService.broadcastJobUpdate(lob,jobId);
        } catch (Exception e) {
            log.error("Error handling job data change", e);
        }
    }

    private void handleFileUpdate(DataChangeEvent event, SSEService sseService) {
        try {
            String lob = event.getLobId();
            String jobId = extractJobIdFromEntity(event.getEntity());
            String masterName = extractMasterName(event.getEntity());
            String fileId = extractFileId(event.getEntity());

            sseService.broadcastFileUpdate(lob, masterName, jobId, fileId);

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