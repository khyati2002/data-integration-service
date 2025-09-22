package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.dto.file.progress.FileProgressResponse;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.dto.event.FileProgressEvent;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.exception.error.ApiError;
import com.salescode.dis.insights.kafka.KafkaProgressPublisher;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.repository.FileStageMetricsRepository;
import com.salescode.dis.insights.service.FileService;
import com.salescode.dis.insights.service.StageService;
import com.salescode.dis.insights.validation.ValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/{lob}")
@RequiredArgsConstructor
@Slf4j
public class FileProgressController {
    private final FileService fileService;
    private final KafkaTemplate<String, FileProgressEvent> kafkaTemplate;
    private final FileStageMetricsRepository fileStageMetricsRepository;
    private final FileRepository fileRepository;
    private final ValidationService validationService;
    private final StageService stageService;
    private final KafkaProgressPublisher kafkaProgressPublisher;
    @Value("${file.progress.update.topic:file-progress-updates}")
    private String fileUpdatesTopic;

    @Operation(summary = "Update file progress", description = "Updates the progress metrics for a file")
    @ApiResponse(responseCode = "202", description = "Progress update accepted", content = @Content(schema = @Schema(implementation = FileProgressResponse.class)))
    @ApiResponse(responseCode = "404", description = "File not found")
    @PutMapping("/master/{master_name}/unit/{fileId}/progress")
    public ResponseEntity<FileProgressResponse> updateFileProgress(
            @PathVariable String lob,
            @PathVariable("master_name") String masterName,
            @PathVariable String fileId,
            @Validated @RequestBody FileProgressRequest progress
    ) {

      if(progress.getModeOfIntegration()== ModeOfIntegration.CK_MDM_KAFKA) {
          try {
              fileId = new String(Base64.getUrlDecoder().decode(fileId), StandardCharsets.UTF_8);
          } catch (Exception ignored) {
              log.warn("Invalid Base64 fileId for CK_MDM_KAFKA: fileId={}", fileId, ignored);
          }
      }
        String eventId = UUID.randomUUID().toString();
        kafkaProgressPublisher.sendKafkaMessage(fileUpdatesTopic, fileId, progress, lob, masterName, eventId);
        FileProgressResponse response = FileProgressResponse.builder()
                .requestId(eventId)
                .status("ACCEPTED")
                .message("Progress update has been queued")
                .fileId(fileId)
                .master(masterName)
                .modeOfIntegration(progress.getModeOfIntegration())
                .build();
        return ResponseEntity.accepted().body(response);
    }


        @Operation(
                summary = "Update file stage metrics status",
                description = """
            Updates the status of a FileStageMetrics entry by its ID.
            
            The status must be one of the allowed values from the ProgressStatus enum:
            PENDING, RUNNING, COMPLETED_SUCCESSFULLY, COMPLETED_UNSUCCESSFULLY, FAILED, ABORTED.
            """
        )
        @ApiResponse(
                responseCode = "200",
                description = "Stage metrics status updated successfully",
                content = @Content(schema = @Schema(implementation = FileStageMetrics.class))
        )
        @ApiResponse(
                responseCode = "404",
                description = "Stage metrics not found with the given ID",
                content = @Content(schema = @Schema(implementation = ApiError.class))
        )
        @PutMapping("/master/{master_name}/{id}/status/{status}")
        public ResponseEntity<String> updateStageStatus(
                @PathVariable String id,
                @PathVariable ProgressStatus status
        ) {
            log.info("Updating FileStageMetrics with id={} to status={}", id, status);

            FileStageMetrics metrics = fileStageMetricsRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("FileStageMetrics not found with id: " + id));

            metrics.setProgressStatus(status);
            FileStageMetrics updated = fileStageMetricsRepository.save(metrics);
            return ResponseEntity.ok("Status updated successfully for id " + id);
        }

    @Operation(
            summary = "Mark stale pending stage metrics as failed",
            description = """
        Finds all FileStageMetrics for a specific LOB with PENDING status that haven't been 
        modified in the last hour and updates their status to FAILED.
        Returns a simple string message with the count of updated stage metrics.
        """)
    @ApiResponse(
            responseCode = "200",
            description = "Stage metrics successfully updated to FAILED status",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid LOB parameter",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Internal server error occurred",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @PutMapping(value = "/stage-metrics/pending", produces = "text/plain")
    public ResponseEntity<String> timeoutPendingStageMetrics(@PathVariable String lob) {

        if (lob == null || lob.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Error: LOB parameter is required");
        }

        try {
            // Calculate cutoff time (1 hour ago) using Instant
            Instant cutoffTime = Instant.now().minus(1, ChronoUnit.HOURS);

            log.info("Finding stale PENDING stage metrics for LOB {} older than {}", lob, cutoffTime);

            // Update the stage metrics to FAILED status
            int updatedCount = stageService.updateStaleStageMetrics(
                    lob, ProgressStatus.PENDING, ProgressStatus.FAILED, cutoffTime);

            log.info("Updated {} stale PENDING stage metrics to FAILED for LOB {}", updatedCount, lob);

            // Return simple string message with count
            String message = String.format("Successfully updated %d stale PENDING stage metrics to FAILED status for LOB: %s",
                    updatedCount, lob);

            return ResponseEntity.ok(message);

        } catch (Exception e) {
            log.error("Error updating stale PENDING stage metrics for LOB {}: {}", lob, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: Failed to update stale stage metrics - " + e.getMessage());
        }
    }


}
