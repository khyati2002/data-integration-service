package com.salescode.dis.insights.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dis.insights.dto.FileProgressRequest;
import com.salescode.dis.insights.dto.UpdateRequestResponseDto;
import com.salescode.dis.insights.kafka.FileProgressEvent;
import com.salescode.dis.insights.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/{lob}/master/{master_name}")
@RequiredArgsConstructor
@Slf4j
public class FileProgressController {
    private final FileService fileService;
    private final KafkaTemplate<String, FileProgressEvent> kafkaTemplate;

    @Operation(summary = "Update file progress", description = "Updates the progress metrics for a file")
    @ApiResponse(responseCode = "202", description = "Progress update accepted", content = @Content(schema = @Schema(implementation = UpdateRequestResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "File not found")
    @PutMapping("/unit/{fileId}/progress")
    public ResponseEntity<UpdateRequestResponseDto> updateFileProgress(
            @PathVariable String lob,
            @PathVariable("master_name") String masterName,
            @PathVariable String fileId,
            @Validated @RequestBody FileProgressRequest progress) {

        if (!fileService.fileExists(fileId,masterName)) {
            return ResponseEntity.notFound().build();
        }

        FileProgressEvent event = new FileProgressEvent();
        String eventId = UUID.randomUUID().toString();
        event.setEventId(eventId);
        event.setFileId(fileId);
        event.setLob(lob);
        event.setMasterName(masterName);
        event.setProgress(progress);
        event.setJobId(null);
        // job is already mapped to a file, hence not required to send

        kafkaTemplate.send("file-progress-updates",fileId, event);

        UpdateRequestResponseDto response = new UpdateRequestResponseDto();
        response.setRequestId(event.getEventId());
        response.setStatus("ACCEPTED");
        response.setMessage("Progress update has been queued");
        response.setFileId(fileId);

        return ResponseEntity.accepted().body(response);
    }
} 