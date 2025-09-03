package com.salescode.dis.insights.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dis.insights.dto.file.FileEntityResponseDto;
import com.salescode.dis.insights.dto.job.JobEntityResponseDto;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.mapper.FileEntityMapper;
import com.salescode.dis.insights.mapper.JobEntityMapper;
import com.salescode.dis.insights.service.FileService;
import com.salescode.dis.insights.service.JobService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class SSEService {

    private final JobService jobService;
    private final ObjectMapper objectMapper;
    private final Map<String, String> fileSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, String> jobSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> fileEmitters = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> jobEmitters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CopyOnWriteArraySet<SseEmitter>> statsEmitters = new ConcurrentHashMap<>();
    private final ExecutorService sendExecutor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler;
    private final JobEntityMapper jobEntityMapper;
    private final FileService fileService;
    private final FileEntityMapper fileEntityMapper;

    @Value("${sse.cleanup.initial-delay:1}")
    private long cleanupInitialDelayMinutes;

    @Value("${sse.cleanup.interval:5}")
    private long cleanupIntervalMinutes;

    public SSEService(JobService jobService, ObjectMapper objectMapper, JobEntityMapper jobEntityMapper, FileService fileService, FileEntityMapper fileEntityMapper) {
        this.jobService = jobService;
        this.objectMapper = objectMapper;
        this.jobEntityMapper = jobEntityMapper;
        this.fileService = fileService;
        this.fileEntityMapper = fileEntityMapper;
        this.scheduler = new DelegatingSecurityContextScheduledExecutorService(Executors.newScheduledThreadPool(1));
    }

    public void addJobEmitter(String clientId, String jobId, SseEmitter emitter) {
        String key = "job:" + clientId ;
        SseEmitter existingEmitter = jobEmitters.remove(key);
        if (existingEmitter != null) {
            log.info("Replacing existing Job SSE connection for client={}, jobId={}", clientId, jobId);
        }
        jobEmitters.put(key, emitter);
        jobSubscriptions.put(key, jobId);
        log.info("Added Job SSE emitter for client={}, jobId={}. Total job connections={}",
                clientId, jobId, jobEmitters.size());
    }

    public void addFileEmitter(String clientId, String fileId, SseEmitter emitter) {
        String key = "file:" + clientId ;

        SseEmitter existingEmitter = fileEmitters.remove(key);
        if (existingEmitter != null) {
            log.info("Replacing existing File SSE connection for client={}, fileId={}", clientId, fileId);
        }
        fileEmitters.put(key, emitter);
        fileSubscriptions.put(key, fileId);
        log.info("Added File SSE emitter for client={}, fileId={}. Total file connections={}",
                clientId, fileId, fileEmitters.size());
    }

    public SseEmitter addStatsEmitter(String key, long timeoutMillis) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        statsEmitters.computeIfAbsent(key, k -> new CopyOnWriteArraySet<>()).add(emitter);
        emitter.onCompletion(() -> removeStatsEmitter(key, emitter));
        emitter.onTimeout(() -> removeStatsEmitter(key, emitter));
        emitter.onError((ex) -> removeStatsEmitter(key, emitter));
        return emitter;
    }

    public void removeEmitter(String clientId, String id, boolean isJob) {
        String key = (isJob ? "job:" : "file:") + clientId + ":" + id;
        Map<String, SseEmitter> targetEmitters = isJob ? jobEmitters : fileEmitters;
        Map<String, String> targetSubscriptions = isJob ? jobSubscriptions : fileSubscriptions;

        SseEmitter existingEmitter = targetEmitters.remove(key);
        targetSubscriptions.remove(key);

        if (existingEmitter != null) {
            log.info("Removed {} SSE emitter for client={}, id={}. Remaining {} connections={}",
                    isJob ? "Job" : "File", clientId, id,
                    isJob ? "job" : "file", targetEmitters.size());
        } else {
            log.debug("No {} emitter found to remove for client={}, id={}",
                    isJob ? "Job" : "File", clientId, id);
        }
    }

    public void removeStatsEmitter(String key, SseEmitter emitter) {
        Set<SseEmitter> set = statsEmitters.get(key);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) {
                statsEmitters.remove(key);
            }
        }
    }

    public void broadcastJobUpdate(String lob, String jobId) {
        JobEntity job = jobService.getJob(jobId);
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        Iterator<Map.Entry<String, SseEmitter>> it = jobEmitters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, SseEmitter> entry = it.next();
            String key = entry.getKey();
            SseEmitter emitter = entry.getValue();
            String encodedJobId = Base64.getEncoder().encodeToString(jobId.getBytes());
            String subscribedJob = jobSubscriptions.get(key);
            if (encodedJobId.equals(subscribedJob)) {
                try {
                    sendSSEEvent(emitter, "individual-job-update", dto, Arrays.asList("individual-job", lob, encodedJobId), key);
                } catch (IOException e) {
                    log.warn("Failed to send job update to {}. Removing connection.", key);
                    it.remove();
                    jobSubscriptions.remove(key);
                }
            }
        }
    }

    public void broadcastFileUpdate(String lob, String masterName, String jobId, String fileId) {
        FileEntity file = fileService.get(fileId, masterName);
        FileEntityResponseDto dto = fileEntityMapper.toDto(file);

        List<Object> queryKey = Arrays.asList("fileDetail", lob, masterName, jobId, fileId);

        Iterator<Map.Entry<String, SseEmitter>> it = fileEmitters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, SseEmitter> entry = it.next();
            String key = entry.getKey();
            SseEmitter emitter = entry.getValue();
            String encodedFileId = Base64.getEncoder().encodeToString(fileId.getBytes());
            String subscribedFile = fileSubscriptions.get(key);
            if (encodedFileId.equals(subscribedFile)) {
                try {
                    sendSSEEvent(emitter, "file-detail-update", dto, queryKey, key);
                } catch (IOException e) {
                    log.warn("Failed to send file update to {}. Removing connection.", key);
                    it.remove();
                    fileSubscriptions.remove(key);
                }
            }
        }
    }

    public void broadcastStatsUpdate(String key, Object payload, String eventName) {
        Set<SseEmitter> set = statsEmitters.get(key);
        if (set == null || set.isEmpty()) return;

        for (SseEmitter emitter : set) {
            sendExecutor.submit(() -> {
                try {
                    SseEmitter.SseEventBuilder event = SseEmitter.event()
                            .name(eventName)
                            .data(payload, MediaType.APPLICATION_JSON);
                    emitter.send(event);
                } catch (IOException e) {
                    removeStatsEmitter(key, emitter);
                }
            });
        }
    }

    private void sendSSEEvent(SseEmitter emitter, String eventType, Object data, List<Object> queryKey, String mapKey) throws IOException {
        if (emitter == null) {
            throw new IOException("Cannot send event to a null emitter for key: " + mapKey);
        }
        try {
            final List<String> finalQueryKey = queryKey == null ? Collections.emptyList() : queryKey.stream().map(String::valueOf).toList();
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("type", eventType);
            eventData.put("data", data);
            eventData.put("queryKey", finalQueryKey);
            eventData.put("timestamp", System.currentTimeMillis());
            String jsonData = objectMapper.writeValueAsString(eventData);

            emitter.send(SseEmitter.event().name("message").data(jsonData).id(UUID.randomUUID().toString()));
            log.debug(" Sent SSE event: {} with queryKey: {}", eventType, queryKey);
        } catch (ClientAbortException | AsyncRequestNotUsableException | IllegalStateException ex) {
            log.warn("Emitter already completed, removing it");
        } catch (IOException e) {
            log.warn("Emitter send failed, removing it", e);
        } catch (Exception ex) {
            log.debug("Emitter error for key={}, removing (cause: {})", mapKey, ex.toString());
        }
    }

    public void sendConnectionEstablished(SseEmitter emitter) throws IOException {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("type", "connection");
        eventData.put("data", "established");
        eventData.put("timestamp", System.currentTimeMillis());
        try {
            String jsonData = objectMapper.writeValueAsString(eventData);
            if (emitter == null) {
                log.info("Emitter is null or complete ");
                return;
            }
            emitter.send(SseEmitter.event().name("message").data(jsonData).id(UUID.randomUUID().toString()));
            log.info("Sent connection established event");
        } catch (IllegalStateException e) {
            log.warn("Emitter already completed, cannot send connection established event", e);
        } catch (ClientAbortException | AsyncRequestNotUsableException e) {
            log.debug("Client disconnected during connection established", e);
        } catch (IOException e) {
            log.warn("IO error sending connection established event", e);
        } catch (Exception e) {
            log.error("Unexpected error sending connection established event", e);
        }
    }

    @PostConstruct
    private void startCleanupTask() {
        scheduler.scheduleAtFixedRate(this::cleanupDeadConnections, cleanupInitialDelayMinutes, cleanupIntervalMinutes, TimeUnit.MINUTES);
    }

    public int getActiveConnectionsCount() {
        cleanupDeadConnections();
        return fileEmitters.size()+ jobEmitters.size();
    }
    public Set<String> getActiveStatsConnections() {
        return statsEmitters.keySet();
    }

    private void cleanupDeadConnections() {
        cleanupMap(fileEmitters, fileSubscriptions, "File");
        cleanupMap(jobEmitters, jobSubscriptions, "Job");
    }

    private void cleanupMap(Map<String, SseEmitter> emitters, Map<String, String> subscriptions, String type) {
        List<String> deadKeys = new ArrayList<>();
        emitters.forEach((key, emitter) -> {
            if (emitter.getTimeout() != null && emitter.getTimeout() <= 0) {
                deadKeys.add(key);
            } else {
                try {
                    emitter.send(SseEmitter.event().comment(""));
                } catch (IllegalStateException | IOException e) {
                    deadKeys.add(key);
                } catch (Exception e) {
                    log.debug("Connection check failed for {}: {}", key, e.getMessage());
                    deadKeys.add(key);
                }
            }
        });
        deadKeys.forEach(key -> {
            SseEmitter e = emitters.remove(key);
            subscriptions.remove(key);
            log.info("Removed dead {} connection: {}", type, key);
        });
    }


    @PreDestroy
    public void cleanup() {
        fileEmitters.clear();
        jobEmitters.clear();
        scheduler.shutdownNow();
        try {
            scheduler.shutdownNow();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Scheduler did not terminate gracefully");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while shutting down scheduler");
        }
        log.info("SSE service shutdown complete");
    }

}