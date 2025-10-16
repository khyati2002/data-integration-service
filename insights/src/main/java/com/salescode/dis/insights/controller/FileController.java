package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.file.FileEntityRequestDto;
import com.salescode.dis.insights.dto.file.FileEntityResponseDto;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileReportEntity;
import com.salescode.dis.insights.entity.mapped.TimeAwareEntity;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.exception.error.ApiError;
import com.salescode.dis.insights.mapper.FileEntityMapper;
import com.salescode.dis.insights.repository.FileReportRepository;
import com.salescode.dis.insights.service.FileService;
import com.salescode.dis.insights.service.S3ExportService;
import com.salescode.dis.insights.sse.SSEService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/{lob}")
@RequiredArgsConstructor
@Slf4j
public class FileController {
    private final FileService fileService;
    private final FileEntityMapper fileEntityMapper;
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private final S3ExportService s3ExportService;
    private final FileReportRepository fileReportRepository;
    private final SSEService sseService;

    @Operation(summary = "Register a new file", description = "Registers a new file entity for the job.")
    @ApiResponse(responseCode = "201", description = "File created successfully", content = @Content(schema = @Schema(implementation = FileEntityResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/master/{master_name}/job/{jobId}/unit")
    public ResponseEntity<FileEntityResponseDto> registerFile(@PathVariable String lob, @PathVariable("master_name") String masterName, @PathVariable String jobId, @Validated @RequestBody FileEntityRequestDto req) {
        FileEntity toSave = fileEntityMapper.toEntity(req, lob, masterName);
        FileEntity saved = fileService.createFile(jobId, toSave);
        FileEntityResponseDto resp = fileEntityMapper.toDto(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @Operation(summary = "Update an existing file id", description = "Updates an existing file entity for the job.")
    @ApiResponse(responseCode = "200", description = "File updated successfully", content = @Content(schema = @Schema(implementation = FileEntityResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "File not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PutMapping("/master/{master_name}/job/{jobId}")
    public ResponseEntity<FileEntityResponseDto> updateFile(
            @PathVariable String lob,
            @PathVariable("master_name") String masterName,
            @PathVariable String jobId,
            @Validated @RequestBody FileEntityRequestDto req) {

        FileEntity updated = fileService.updateFile(jobId, masterName,req, lob);
        FileEntityResponseDto resp = fileEntityMapper.toDto(updated);
        return ResponseEntity.ok(resp);
    }


    @Operation(summary = "Get a specific file by ID", description = "Fetch a file by its unique ID.")
    @ApiResponse(responseCode = "200", description = "File found", content = @Content(schema = @Schema(implementation = FileEntityResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "File not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/master/{master_name}/job/{jobId}/unit/{fileId}")
    public ResponseEntity<FileEntityResponseDto> getFile(@PathVariable String lob, @PathVariable("master_name") String masterName, @PathVariable String jobId, @PathVariable String fileId) {
        String decodedFileId = fileId;
        try {
            decodedFileId = new String(Base64.getDecoder().decode(fileId));
        }
        catch (Exception e){}
        FileEntity file = fileService.getFile(decodedFileId, masterName);
        FileEntityResponseDto resp = fileEntityMapper.toDto(file);
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Get all files for a specific job", description = "Retrieve a paginated list of all files associated with a specific job.")
    @ApiResponse(responseCode = "200", description = "List of files for the job", content = @Content(schema = @Schema(implementation = FileEntityResponseDto.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/job/{jobId}/unit")
    public ResponseEntity<Page<FileEntityResponseDto>> listByJob(@PathVariable String lob, @PathVariable String jobId, Pageable pageable) {
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSortOr(Sort.by(Sort.Direction.DESC, TimeAwareEntity.START_TIME)));
        Page<FileEntityResponseDto> pageRes = fileService.listByJob(jobId, pageRequest).map(fileEntityMapper::toDto);
        return ResponseEntity.ok(pageRes);
    }

    @Operation(summary = "Set total count for a file", description = "Updates the total count for a specific file.")
    @ApiResponse(responseCode = "200", description = "Total count updated successfully", content = @Content(schema = @Schema(implementation = FileEntityResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "File not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "400", description = "Invalid total count value", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PutMapping("/master/{master_name}/unit/{fileId}/total-count")
    public ResponseEntity<FileEntityResponseDto> setTotalCount(
            @PathVariable String lob,
            @PathVariable("master_name") String masterName,
            @PathVariable String fileId,
            @RequestParam Long totalCount) {

        fileId = decodeIfBase64(fileId);
        log.info("Setting total count {} for file {}  for master {}", totalCount, fileId, masterName);

        FileEntity updatedFile = fileService.setTotalCount(fileId, masterName, totalCount);
        FileEntityResponseDto resp = fileEntityMapper.toDto(updatedFile);
        return ResponseEntity.ok(resp);
    }
    private String decodeIfBase64(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            return fileId;
        }
        // Skip if UUID (with or without hyphens)
        if (fileId.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") ||
                fileId.matches("^[0-9a-fA-F]{32}$")) {
            return fileId;
        }
        // Skip if not Base64 URL-safe pattern
        if (!fileId.matches("^[A-Za-z0-9_-]+$")) {
            return fileId;
        }
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(fileId);
            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
            // Only return decoded if printable
            if (decoded.chars().allMatch(c -> c >= 32 && c < 127)) {
                return decoded;
            }
        } catch (IllegalArgumentException e) {
            // not valid Base64 — return original
        }
        return fileId;
    }

    @GetMapping("/report/{fileId}")
    public ResponseEntity<FileReportEntity> checkFileExists(@PathVariable String fileId) {
        if (fileId == null || fileId.trim().isEmpty()) {
            logger.warn("Received a request with a blank or null fileId.");
            return ResponseEntity.badRequest().build();
        }
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(fileId);
            fileId = new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Optional<FileReportEntity> fileReportOptional = fileReportRepository.findByFileId(fileId);
            if (fileReportOptional.isPresent() &&
                    ("COMPLETED".equals(fileReportOptional.get().getStatus()) ||
                            "FAILED".equals(fileReportOptional.get().getStatus()))
            ) {
                logger.info("File with fileId '{}' found with status COMPLETED or FAILED. Returning OK.", fileId);
                return ResponseEntity.ok(fileReportOptional.get());
            } else {
                logger.info("File with fileId '{}' not found. Returning NOT_FOUND.", fileId);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error checking existence for fileId: {}", fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/report/generate")
    public ResponseEntity<Object> startFailureExport(@RequestBody Map<String, String> payload) {
        String fileId = payload.get("fileId");
        String lob = payload.get("lob");
        String entity = payload.get("entity");

        if (fileId == null || fileId.trim().isEmpty() ||
                lob == null || lob.trim().isEmpty() ||
                entity == null || entity.trim().isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(Collections.singletonMap("error", "'fileId', 'lob' and 'entity' field must be provided."));
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(fileId);
            fileId = new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Collections.singletonMap("error", "Invalid Base64 encoding for 'fileId'."));
        }

        try {
            Optional<FileReportEntity> existingReport = fileReportRepository.findByFileId(fileId);

            if (existingReport.isPresent()) {
                FileReportEntity report = existingReport.get();

                if ("COMPLETED".equals(report.getStatus())) {
                    logger.info("Found existing report URL for fileId '{}' in database.", fileId);
                    return ResponseEntity.ok(Collections.singletonMap("fileReport", report));
                }

                if ("FAILED".equals(report.getStatus())) {
                    logger.info("Retrying export for fileId '{}'. Resetting status to IN_PROGRESS.", fileId);
                    report.setStatus("IN_PROGRESS");
                    report.setErrorMessage(null);
                    report.setUrl(null);
                    fileReportRepository.saveAndFlush(report);
                    s3ExportService.exportFailuresAsync(fileId,lob,entity);
                    return ResponseEntity.ok(Collections.singletonMap("fileReport", report));
                }

                logger.info("Report for fileId '{}' already in status '{}'. Returning existing.", fileId, report.getStatus());
                return ResponseEntity.ok(Collections.singletonMap("fileReport", report));
            }

            logger.info("No existing report found for fileId '{}'. Creating new export entry.", fileId);
            FileReportEntity newReport = fileService.createReportEntry(fileId);
            s3ExportService.exportFailuresAsync(fileId,lob,entity);

            return ResponseEntity.ok(Collections.singletonMap("fileReport", newReport));

        } catch (Exception e) {
            logger.error("An unexpected error occurred during the export process for fileId: {}", fileId, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "An internal server error occurred."));
        }
    }


    @GetMapping("/reports/{fileId}/events")
    public SseEmitter streamReportEvents(@PathVariable String fileId) {
        long timeout = 10 * 60 * 1000L;

        byte[] decodedBytes = Base64.getDecoder().decode(fileId);
        fileId = new String(decodedBytes, StandardCharsets.UTF_8);
        SseEmitter emitter = sseService.addFileReportEmitter(fileId, timeout);

        try {

            emitter.send(SseEmitter.event()
                    .name("report-update")
                    .id(UUID.randomUUID().toString())
                    .data(Map.of("report-update","started" ), MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            sseService.removeReportEmitter(fileId, emitter);
            logger.error(e.getMessage());
        }
        return emitter;
    }

    @GetMapping("/file/{fileId}/download-url")
    public ResponseEntity<?> getDownloadUrl(@PathVariable String fileId) {

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(fileId);
            fileId = new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Collections.singletonMap("error", "Invalid Base64 encoding for 'fileId'."));
        }

        return fileReportRepository.findByFileId(fileId)
                .map(report -> {
                    if (report.getUrl() == null || report.getUrl().isEmpty()) {
                        return ResponseEntity.notFound().build();
                    }

                    try {
                        long expirationMillis = 5 * 60 * 1000;
                        URL presignedUrl = s3ExportService.generatePresignedUrl(report.getUrl(), expirationMillis);

                        return ResponseEntity.ok(Map.of("url", presignedUrl.toString()));
                    } catch (Exception e) {
                        return ResponseEntity.internalServerError().body("Error generating download link.");
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Update file status",
            description = """
        Updates the processing status of a specific file.
        
        The status must be one of the allowed values from the FileStatus enum 
        (e.g., PENDING, PROCESSING, COMPLETED, FAILED).
        """)
    @ApiResponse(
            responseCode = "200",
            description = "File status updated successfully",
            content = @Content(schema = @Schema(implementation = FileEntityResponseDto.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid status value or bad request",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "File not found with the specified ID",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Internal server error occurred while updating file status",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @PutMapping("/master/{master_name}/unit/{id}/status/{status}")
    public ResponseEntity<FileEntityResponseDto> updateFileStatus(
            @PathVariable String lob,
            @PathVariable("master_name") String masterName,
            @PathVariable String id,
            @PathVariable ProgressStatus status) {

        log.info("Updating status of file {} in master {} to {}", id, masterName, status);

        FileEntity updatedFile = fileService.updateStatus(id, masterName, status);
        FileEntityResponseDto resp = fileEntityMapper.toDto(updatedFile);
        return ResponseEntity.ok(resp);
    }

    @Operation(
            summary = "Mark stale pending files as failed",
            description = """
        Finds all files for a specific LOB with PENDING status that haven't been 
        modified in the last hour and updates their status to FAILED.
        Returns a simple string message with the count of updated files.
        """)
    @ApiResponse(
            responseCode = "200",
            description = "Files successfully updated to FAILED status",
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
    @PutMapping(value = "/files/pending", produces = "text/plain")
    public ResponseEntity<String> timeoutPendingFiles(@PathVariable String lob) {

        if (lob == null || lob.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Error: LOB parameter is required");
        }

        try {
            Instant cutoffTime = Instant.now().minus(1, ChronoUnit.HOURS);

            log.info("Finding stale PENDING files for LOB {} older than {}", lob, cutoffTime);

            // Update the files to FAILED status
            int updatedCount = fileService.updateStaleFiles(
                    lob, ProgressStatus.PENDING, ProgressStatus.FAILED, cutoffTime);

            log.info("Updated {} stale PENDING files to FAILED for LOB {}", updatedCount, lob);

            // Return simple string message with count
            String message = String.format("Successfully updated %d stale PENDING files to FAILED status for LOB: %s",
                    updatedCount, lob);

            return ResponseEntity.ok(message);

        } catch (Exception e) {
            log.error("Error updating stale PENDING files for LOB {}: {}", lob, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: Failed to update stale files - " + e.getMessage());
        }
    }



}