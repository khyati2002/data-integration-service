package com.salescode.dis.insights.SSE;

import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.entity.StageMetadata;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
public class DataChangeListener {

    private final ApplicationEventPublisher eventPublisher;
    private static final ThreadLocal<AtomicBoolean> PROCESSING_FLAG =
            ThreadLocal.withInitial(() -> new AtomicBoolean(false));

    @PostPersist
    @PostUpdate
    @PostRemove
    public void handleDataChange(Object entity) {
        // Prevent recursive calls
        if (PROCESSING_FLAG.get().get()) {
            log.debug("Skipping recursive data change event for: {}", entity.getClass().getSimpleName());
            return;
        }
        log.debug("Data change detected for entity: {}", entity.getClass().getSimpleName());
        String eventType = determineEventType(entity);

        if (eventType != null) {
            String lob = extractLobFromEntity(entity);
            // Publish event for async processing AFTER transaction commits
            DataChangeEvent event = new DataChangeEvent(eventType, entity, lob);
            eventPublisher.publishEvent(event);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAsyncDataChange(DataChangeEvent event) {
        PROCESSING_FLAG.get().set(true);
        try {
            log.info("Handling async data change event: {}", event.getEventType());

            SSEService sseService = ApplicationContextHolder.getContext().getBean(SSEService.class);

            switch (event.getEventType()) {
                case "JOB_UPDATE":
                    handleAllJobDataChange(event, sseService);
                    break;
                case "SUMMARY_UPDATE":
                    handleLobSummaryDataChange(event, sseService);
                    break;
                case "STAGE_UPDATE":
                    handleStageDataChange(sseService);
                    break;
                case "FILE_UPDATE":
                    handleAllJobDataChange(event, sseService);
                    handleFileUpdate(event, sseService);
                    handleLobSummaryDataChange(event, sseService);
                    break;
            }
        } catch (Exception e) {
            log.error("Error handling async data change event: {}", event.getEventType(), e);
        } finally {
            PROCESSING_FLAG.get().set(false);
        }
    }

    private String determineEventType(Object entity) {
        if (entity instanceof JobEntity) return "JOB_UPDATE";
        if (entity instanceof FileEntity ) return "FILE_UPDATE";
        if (entity instanceof FileStageMetrics) return "SUMMARY-UPDATE";
        if (entity instanceof StageMetadata)return "STAGE_UPDATE";
        return null;
    }

    private void handleAllJobDataChange(DataChangeEvent event, SSEService sseService) {
        try {
            String lob = event.getLobId();
            String jobId = extractJobIdFromEntity(event.getEntity());
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(10);

            sseService.broadcastAllJobsUpdate(lob,null, startDate, endDate, jobId);
        } catch (Exception e) {
            log.error("Error handling job data change", e);
        }
    }
    private void handleLobSummaryDataChange(DataChangeEvent event, SSEService sseService) {
        try {
            String lob = event.getLobId();
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(10);

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
            String masterName=extractMasterName(event.getEntity());
            String fileId=extractFileId(event.getEntity());

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
        return  null;
    }
    private String extractFileId(Object entity) {
        return ((FileEntity) entity).getFileId();
    }
    private String extractMasterName(Object entity) {
        return ((FileEntity) entity).getMaster();
    }

    @PreDestroy
    public void cleanup() {
        PROCESSING_FLAG.remove();
    }

    @Getter
    public static class DataChangeEvent {
        // Getters
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
