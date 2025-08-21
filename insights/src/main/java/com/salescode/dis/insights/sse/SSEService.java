package com.salescode.dis.insights.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dis.insights.dto.AccumulatedJobsAndMasterDto;
import com.salescode.dis.insights.dto.LobSummaryDto;
import com.salescode.dis.insights.dto.StageDto;
import com.salescode.dis.insights.dto.file.FileEntityResponseDto;
import com.salescode.dis.insights.dto.job.JobEntityResponseDto;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.mapper.FileEntityMapper;
import com.salescode.dis.insights.mapper.JobEntityMapper;
import com.salescode.dis.insights.repository.FileStageMetricsRepository;
import com.salescode.dis.insights.service.FileService;
import com.salescode.dis.insights.service.JobService;
import com.salescode.dis.insights.service.StageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SSEService {

    private final FileStageMetricsRepository fileStageMetricsRepository;
    private final JobService jobService;
    private final StageService stageService;
    private final ObjectMapper objectMapper;
    private final Map<String, SseEmitter> clientEmitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final JobEntityMapper jobEntityMapper;
    private final FileService fileService;
    private final FileEntityMapper fileEntityMapper;

    public SSEService(FileStageMetricsRepository fileStageMetricsRepository, JobService jobService, StageService stageService, ObjectMapper objectMapper, JobEntityMapper jobEntityMapper, FileService  fileService, FileEntityMapper fileEntityMapper)
    {
        this.fileStageMetricsRepository = fileStageMetricsRepository;
        this.jobService = jobService;
        this.stageService = stageService;
        this.objectMapper = objectMapper;
        this.jobEntityMapper=jobEntityMapper;
        this.fileService=fileService;
        this.fileEntityMapper=fileEntityMapper;
        startCleanupTask();
    }

    public void addEmitter(String clientId, SseEmitter emitter) {
        SseEmitter existingEmitter = clientEmitters.get(clientId);
        if (existingEmitter != null) {
            try {
                log.info("Replacing existing SSE connection for client: {}", clientId);
                existingEmitter.complete();
            } catch (Exception e) {
                log.warn("Error closing existing emitter", e);
            }
        }
        clientEmitters.put(clientId, emitter);
        log.info("✅ Added SSE emitter for client: {}. Total connections: {}",
                clientId, clientEmitters.size());
    }

    public void removeEmitter(String clientId, SseEmitter emitter) {
        SseEmitter existingEmitter = clientEmitters.get(clientId);
        if (existingEmitter == emitter) {
            clientEmitters.remove(clientId);
            log.info("❌ Removed SSE emitter for client: {}. Total connections: {}",
                    clientId, clientEmitters.size());
        }
    }

    public int getActiveConnectionsCount() {
        cleanupDeadConnections();
        return clientEmitters.size();
    }

    private void cleanupDeadConnections() {
        Iterator<Map.Entry<String, SseEmitter>> iterator = clientEmitters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, SseEmitter> entry = iterator.next();
            SseEmitter emitter = entry.getValue();

            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Exception e) {
                log.debug("Removing dead SSE connection for client: {}", entry.getKey());
                iterator.remove();
            }
        }
    }

    public void sendInitialData(String clientId, SseEmitter emitter) {
        try {
            log.info("📤 Sending initial data for client: {}", clientId);
            emitter.send(SseEmitter.event().comment("Connected - sending initial data"));
            sendSSEEvent(emitter, "connection", "established", Arrays.asList("connection", "status"));
            log.info("✅ Initial SSE data sent successfully for client: {}", clientId);
        } catch (Exception e) {
            log.error("❌ Error sending initial SSE data for client: {}", clientId, e);
            throw new RuntimeException("Failed to send initial data", e);
        }
    }


    private List<String> getAllAvailableLabsSafely() {
        try {
            return jobService.getAllAvailableLobs();
        } catch (Exception e) {
            log.error("Error getting available LOBs", e);
            return Collections.emptyList();
        }
    }

    private void sendSSEEvent(SseEmitter emitter, String eventType, Object data, List<Object> queryKey) throws IOException {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("type", eventType);
            eventData.put("data", data);
            eventData.put("queryKey", queryKey);
            eventData.put("timestamp", System.currentTimeMillis());

            String jsonData = objectMapper.writeValueAsString(eventData);

            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(jsonData)
                    .id(UUID.randomUUID().toString()));

            log.debug("📨 Sent SSE event: {} with queryKey: {}", eventType, queryKey);

        } catch (IllegalStateException e) {
            log.warn("Emitter already completed, removing it");

        } catch (IOException e) {
            log.warn("Emitter send failed, removing it", e);
        }
    }


    public void broadcastAllJobsUpdate(String lob, String mode, LocalDateTime startDate, LocalDateTime endDate, String jobId) {
        try {
            AccumulatedJobsAndMasterDto jobsData = jobService.getJobsWithAggregatedStagesAndMasters(
                    lob, startDate, endDate, mode);
            String encodedjobId= Base64.getEncoder().encodeToString(jobId.getBytes());
            List<Object> queryKey = Arrays.asList("all-jobs", lob,"");
            List<Object> queryKey1 = Arrays.asList("individual-job", lob, encodedjobId);

            JobEntity job = jobService.getJob(jobId);
            JobEntityResponseDto dto = jobEntityMapper.toDto(job);

            for (Map.Entry<String, SseEmitter> entry : clientEmitters.entrySet()) {
                String clientId = entry.getKey();
                SseEmitter emitter = entry.getValue();
                if (emitter != null) {
                    try {
                        sendSSEEvent(emitter, "all-jobs-update", jobsData, queryKey);
                        sendSSEEvent(emitter, "individual-job-update", dto, queryKey1);
                        log.debug("📨 Sent jobs update to client: {}", clientId);
                    } catch (IOException e) {
                        log.warn("Failed to send jobs update to client: {}, removing connection", clientId);
                        clientEmitters.remove(clientId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fetching and broadcasting jobs data", e);
        }
    }

    public void broadcastLobSummaryUpdate(String lob, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            Instant startInstant = (startDate != null) ? startDate.atZone(ZoneId.systemDefault()).toInstant()
                    : LocalDateTime.now().minusDays(10).atZone(ZoneId.systemDefault()).toInstant();
            Instant endInstant = (endDate != null) ? endDate.atZone(ZoneId.systemDefault()).toInstant()
                    : LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant();

            List<String> lobs = "ALL".equals(lob) ? getAllAvailableLabsSafely() : Arrays.asList(lob);

            List<LobSummaryDto> summaryData = fileStageMetricsRepository.findAggregateMetricsAndJobCountsByLobAndDateRange(
                    lobs, ProgressStage.QUEUE, ProgressStage.SAVE, startInstant, endInstant);
            List<Object> queryKey1= Arrays.asList("summary-data", lob, startDate, endDate);
            broadcastDataToClients("dashboard-summary-update", summaryData, queryKey1);

        } catch (Exception e) {
            log.error("Error broadcasting LOB summary update", e);
        }
    }

    public  void broadcastStagesData() throws IOException {
        try {
            List<StageDto> stagesData = stageService.getStages();
            List<Object> queryKey = Arrays.asList("stages");
            broadcastDataToClients("stages", stagesData, queryKey);
        } catch (Exception e) {
            log.error("Error sending stages data via SSE", e);
        }
    }
    public void broadcastFileDetailUpdate(String lob, String masterName, String jobId, String fileId) {
        try {
            FileEntity file = fileService.get(fileId, masterName);
            FileEntityResponseDto dto = fileEntityMapper.toDto(file);

            List<Object> queryKey = Arrays.asList("fileDetail", lob, masterName, jobId, fileId);
            broadcastDataToClients("file-detail-update", dto, queryKey);
            log.debug("📨 Broadcasted file detail update for fileId: {}", fileId);

        } catch (Exception e) {
            log.error("Error broadcasting file detail update for fileId: {}", fileId, e);
        }
    }


    private void broadcastDataToClients(String eventType, Object data, List<Object> queryKey) {
        List<String> failedClients = new ArrayList<>();

        for (Map.Entry<String, SseEmitter> entry : clientEmitters.entrySet()) {
            String clientId = entry.getKey();
            SseEmitter emitter = entry.getValue();
                try {
                    sendSSEEvent(emitter, eventType, data, queryKey);
                } catch (IOException e) {
                    log.warn("Failed to send SSE event to client: {}, removing connection", clientId);
                    failedClients.add(clientId);
                }
        }
        failedClients.forEach(clientEmitters::remove);
        if (!failedClients.isEmpty()) {
            log.info("Cleaned up {} dead SSE connections", failedClients.size());
        }
    }

    private void startCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupDeadConnections();
            } catch (Exception e) {
                log.error("Error in SSE cleanup task", e);
            }
        }, 60, 60, TimeUnit.SECONDS); // Every minute
    }

    @PreDestroy
    public void cleanup() {
        log.info("🛑 Shutting down SSE service...");

        // Close all connections
        clientEmitters.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("Server shutting down"));
                emitter.complete();
            } catch (Exception e) {
                log.warn("Error closing SSE connection for client: {}", clientId);
            }
        });
        clientEmitters.clear();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("✅ SSE service shutdown complete");
    }
}