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
    @GetMapping(value = "/job-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamJob(@RequestParam String clientId, @RequestParam String jobId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> sseService.removeEmitter(clientId,jobId, true));
        emitter.onTimeout(() -> { sseService.removeEmitter(clientId,jobId, true); emitter.complete(); });
        emitter.onError(ex -> sseService.removeEmitter(clientId,jobId, true));

        sseService.addJobEmitter(clientId, jobId, emitter);
        CompletableFuture.runAsync(() -> {
            try {
                // Small delay to ensure emitter is properly registered
                Thread.sleep(120);
                sseService.sendConnectionEstablished(emitter);
            } catch (IllegalStateException e) {
                // Emitter already completed - this is fine, just log it
                log.debug("Emitter already completed when trying to send connection established message for clientId: {}, jobId: {}", clientId, jobId);
            } catch (Exception e) {
                log.warn("Failed to send connection established event for clientId: {}, jobId: {}", clientId, jobId, e);
            }
        });

        return emitter;
    }

    @GetMapping(value = "/file-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFile(@RequestParam String clientId, @RequestParam String fileId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> sseService.removeEmitter(clientId,fileId, false));
        emitter.onTimeout(() -> {
            sseService.removeEmitter(clientId,fileId, false); emitter.complete();
            try { emitter.complete(); } catch (Exception ignored) {}
        });
        emitter.onError(ex -> sseService.removeEmitter(clientId,fileId, false));

        sseService.addFileEmitter(clientId, fileId, emitter);
        CompletableFuture.runAsync(() -> {
            try {
                // Small delay to ensure emitter is properly registered
                Thread.sleep(120);
                sseService.sendConnectionEstablished(emitter);
            } catch (IllegalStateException e) {
                // Emitter already completed - this is fine, just log it
                log.debug("Emitter already completed when trying to send connection established message for clientId: {}, fileId: {}", clientId, fileId);
            } catch (Exception e) {
                log.warn("Failed to send connection established event for clientId: {}, fileId: {}", clientId, fileId, e);
            }
        });

        return emitter;
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
        public int getActiveConnections() { return activeConnections; }
        public String getStatus() { return status; }
    }
}