package com.salescode.dis.insights.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SSEController {

    private final SSEService sseService;

    @GetMapping(value = "/data-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamData(HttpServletRequest request, @RequestParam String clientId) {

        log.info("✅ New SSE connection established from IP: {}", clientId);
        SseEmitter emitter = new SseEmitter(0L);

        emitter.onCompletion(() -> {
            log.info("SSE connection completed for client: {}", clientId);
            sseService.removeEmitter(clientId, emitter);
        });

        emitter.onTimeout(() -> {
            log.info("SSE connection timeout for client: {}", clientId);
            sseService.removeEmitter(clientId, emitter);
            emitter.complete();
        });

        emitter.onError(throwable -> {
            log.warn("SSE connection error for client: {}, error: {}", clientId, throwable.getMessage());
            sseService.removeEmitter(clientId, emitter);
        });

        sseService.addEmitter(clientId, emitter);

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(100);
                sseService.sendInitialData(clientId, emitter);
            }
            catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Thread interrupted while sending initial SSE data for client: {}", clientId, ie);
            }
            catch (Exception e) {
                log.error("Error sending initial SSE data for client: {}", clientId, e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.error("Error completing emitter with error", ex);
                }
            }
        });

        return emitter;
    }
    @PostMapping("/context")
    public ResponseEntity<String> updateClientContext(
            HttpServletRequest request,
            @RequestParam(required = false) String lob,
            @RequestParam(required = false) String jobId,
            @RequestParam(required = false) String masterId,
            @RequestParam(required = false) String timePeriod,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        String clientId = request.getRemoteAddr();
        log.debug("Updated context for client: {} - LOB: {}, Job: {}, Master: {}, Period: {}, Mode: {}",
                clientId, lob, jobId, masterId, timePeriod, mode);

        return ResponseEntity.ok("Context updated");
    }

    @GetMapping("/status")
    public ResponseEntity<Object> getConnectionStatus() {
        try {
            int activeConnections = sseService.getActiveConnectionsCount();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(new ConnectionStatus(activeConnections, "SSE service is running"));
        } catch (Exception e) {
            log.error("Error getting SSE status", e);
            return ResponseEntity.internalServerError()
                    .body(new ConnectionStatus(0, "Error: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("SSE service is healthy");
    }


    public static class ConnectionStatus {
        public final int activeConnections;
        public final String status;

        public ConnectionStatus(int activeConnections, String status) {
            this.activeConnections = activeConnections;
            this.status = status;
        }

        // Getters for JSON serialization
        public int getActiveConnections() { return activeConnections; }
        public String getStatus() { return status; }
    }
}