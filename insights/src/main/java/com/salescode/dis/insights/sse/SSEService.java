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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SSEService {

    private final JobService jobService;
    private final ObjectMapper objectMapper;
    private final Map<String, String> fileSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, String> jobSubscriptions = new ConcurrentHashMap<>();

    private final Map<String, SseEmitter> fileEmitters = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> jobEmitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final JobEntityMapper jobEntityMapper;
    private final FileService fileService;
    private final FileEntityMapper fileEntityMapper;

    public SSEService( JobService jobService, ObjectMapper objectMapper, JobEntityMapper jobEntityMapper, FileService  fileService, FileEntityMapper fileEntityMapper)
    {
        this.jobService = jobService;
        this.objectMapper = objectMapper;
        this.jobEntityMapper=jobEntityMapper;
        this.fileService=fileService;
        this.fileEntityMapper=fileEntityMapper;
        startCleanupTask();
    }

    public void addJobEmitter(String clientId, String jobId, SseEmitter emitter) {
        String key = "job:" + clientId + ":" + jobId;

        SseEmitter existingEmitter = jobEmitters.get(key);
        if (existingEmitter != null) {
            try {
                log.info("Replacing existing Job SSE connection for client={}, jobId={}", clientId, jobId);
                existingEmitter.complete();
            } catch (Exception e) {
                log.warn("Error closing existing job emitter", e);
            }
        }

        jobEmitters.put(key, emitter);
        jobSubscriptions.put(key, jobId);
        log.info("Added Job SSE emitter for client={}, jobId={}. Total job connections={}",
                clientId, jobId, jobEmitters.size());
    }

    public void addFileEmitter(String clientId, String fileId, SseEmitter emitter) {
        String key = "file:" + clientId + ":" + fileId;

        SseEmitter existingEmitter = fileEmitters.get(key);
        if (existingEmitter != null) {
            try {
                log.info("Replacing existing File SSE connection for client={}, fileId={}", clientId, fileId);
                existingEmitter.complete();
            } catch (Exception e) {
                log.warn("Error closing existing file emitter", e);
            }
        }
        fileEmitters.put(key, emitter);
        fileSubscriptions.put(key, fileId);
        log.info("Added File SSE emitter for client={}, fileId={}. Total file connections={}",
                clientId, fileId, fileEmitters.size());
    }


    public void removeEmitter(String clientId, String id, boolean isJob) {
        String key = (isJob ? "job:" : "file:") + clientId + ":" + id;
        Map<String, SseEmitter> targetEmitters = isJob ? jobEmitters : fileEmitters;
        Map<String, String> targetSubscriptions = isJob ? jobSubscriptions : fileSubscriptions;

        SseEmitter existingEmitter = targetEmitters.remove(key);
        targetSubscriptions.remove(key);

        if (existingEmitter != null) {
            try {
                existingEmitter.complete();
                log.info("Removed {} SSE emitter for client={}, id={}. Remaining {} connections={}",
                        isJob ? "Job" : "File", clientId, id,
                        isJob ? "job" : "file", targetEmitters.size());
            } catch (Exception e) {
                log.warn("Error completing {} emitter for client={}, id={}",
                        isJob ? "Job" : "File", clientId, id, e);
            }
        } else {
            log.debug("No {} emitter found to remove for client={}, id={}",
                    isJob ? "Job" : "File", clientId, id);
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
            String encodedJobId=Base64.getEncoder().encodeToString(jobId.getBytes());
            String subscribedJob = jobSubscriptions.get(key);
            if (encodedJobId.equals(subscribedJob)) {
                try {
                    sendSSEEvent(emitter, "individual-job-update", dto, Arrays.asList("individual-job",log, encodedJobId));
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
            String encodedFileId=Base64.getEncoder().encodeToString(fileId.getBytes());
            String subscribedFile = fileSubscriptions.get(key);
            if (encodedFileId.equals(subscribedFile)) {
                try {
                    sendSSEEvent(emitter, "file-detail-update", dto, queryKey);
                } catch (IOException e) {
                    log.warn("Failed to send file update to {}. Removing connection.", key);
                    it.remove();                 // safe with CHM iterator too
                    fileSubscriptions.remove(key);
                }
            }
        }
    }



    private void sendSSEEvent(SseEmitter emitter, String eventType, Object data, List<Object> queryKey) throws IOException {
        try {
            final List<String> finalqueryKey = queryKey == null ? Collections.emptyList() : queryKey.stream().map(String::valueOf).collect(Collectors.toList());
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("type", eventType);
            eventData.put("data", data);
            eventData.put("queryKey", finalqueryKey);
            eventData.put("timestamp", System.currentTimeMillis());
            String jsonData = objectMapper.writeValueAsString(eventData);

            emitter.send(SseEmitter.event().name("message").data(jsonData).id(UUID.randomUUID().toString()));
            log.debug(" Sent SSE event: {} with queryKey: {}", eventType, queryKey);
        } catch (IllegalStateException e) {
            log.warn("Emitter already completed, removing it");
        } catch (IOException e) {
            log.warn("Emitter send failed, removing it", e);
        }
    }
    public void sendConnectionEstablished(SseEmitter emitter) throws IOException {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("type", "connection");
        eventData.put("data", "established");
        eventData.put("timestamp", System.currentTimeMillis());

        String jsonData = objectMapper.writeValueAsString(eventData);
        emitter.send(SseEmitter.event().name("message").data(jsonData).id(UUID.randomUUID().toString()));
        log.info("Sent connection established event");
    }

    private void startCleanupTask() {
        scheduler.scheduleAtFixedRate(this::cleanupDeadConnections, 60, 60, TimeUnit.SECONDS);
    }

    public int getActiveConnectionsCount() {
        cleanupDeadConnections();
        return fileEmitters.size();
    }

    private void cleanupDeadConnections() {
        cleanupMap(fileEmitters, fileSubscriptions, "File");
        cleanupMap(jobEmitters, jobSubscriptions, "Job");
    }

    private void cleanupMap(Map<String, SseEmitter> emitters, Map<String, String> subscriptions, String type) {
        List<String> deadKeys = new ArrayList<>();
        emitters.forEach((key, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Exception e) {
                deadKeys.add(key);
            }
        });
        deadKeys.forEach(key -> {
            emitters.remove(key);
            subscriptions.remove(key);
            log.info("Removed dead {} connection: {}", type, key);
        });
    }

    @PreDestroy
    public void cleanup() {
        fileEmitters.forEach((k, e) -> safeComplete(e));
        jobEmitters.forEach((k, e) -> safeComplete(e));
        fileEmitters.clear();
        jobEmitters.clear();
        scheduler.shutdownNow();
        log.info("SSE service shutdown complete");
    }

    private void safeComplete(SseEmitter emitter) {
        try { emitter.complete(); } catch (Exception ignored) {}
    }
}