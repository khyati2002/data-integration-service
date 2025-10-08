package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.file.progress.FileProgressResponse;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DuplicatesController {
    private final FileService fileService;
    @Operation(summary = "Update metrics for duplicate counts")
    @ApiResponse(responseCode = "202", description = "Progress update accepted", content = @Content(schema = @Schema(implementation = FileProgressResponse.class)))
    @ApiResponse(responseCode = "404", description = "File not found")
    @PutMapping("/update-duplicate-counts")
    public ResponseEntity<Map<String, Object>> updateDuplicateCounts(@RequestBody DuplicateEventRequest request) {
        FileEntity file=fileService.getFile(request.getFileId(),request.getMasterName());
        FileStageMetrics metrics=fileService.updateMetricsForDuplicates(file, request);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "fileId", request.getFileId(),
                "stageType", request.getStageType(),
                "processedAt", Instant.now().toString(),
                "metrics", metrics
        ));
    }

    @Data
    public static class DuplicateEventRequest {
        private String fileId;
        private ProgressStage stageType;
        private Map<String, Integer> duplicateCounts;
        private String timestamp;
        private String masterName;
    }
}