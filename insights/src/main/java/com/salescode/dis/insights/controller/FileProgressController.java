package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.dto.file.progress.FileProgressResponse;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.dto.event.FileProgressEvent;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.service.FileService;
import com.salescode.dis.insights.validation.ValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/api/{lob}/master/{master_name}")
@RequiredArgsConstructor
@Slf4j
public class FileProgressController {
    private final FileService fileService;
    private final KafkaTemplate<String, FileProgressEvent> kafkaTemplate;
    private final FileRepository fileRepository;
    private final ValidationService validationService;
    @Value("${file.progress.update.topic:file-progress-updates}")
    private String fileUpdatesTopic;

    @Operation(summary = "Update file progress", description = "Updates the progress metrics for a file")
    @ApiResponse(responseCode = "202", description = "Progress update accepted", content = @Content(schema = @Schema(implementation = FileProgressResponse.class)))
    @ApiResponse(responseCode = "404", description = "File not found")
    @PutMapping("/unit/{fileId}/progress")
    public ResponseEntity<FileProgressResponse> updateFileProgress(
            @PathVariable String lob,
            @PathVariable("master_name") String masterName,
            @PathVariable String fileId,
            @Validated @RequestBody FileProgressRequest progress
    ) {

      if(progress.getModeOfIntegration()== ModeOfIntegration.CK_MDM_KAFKA) {
          try {
              fileId = new String(Base64.getUrlDecoder().decode(fileId), StandardCharsets.UTF_8);
          } catch (Exception e) {
          }
      }
        FileEntity file = fileService.get(fileId,masterName);
        FileProgressEvent event = new FileProgressEvent();
        String eventId = UUID.randomUUID().toString();
        event.setEventId(eventId);
        event.setFileId(fileId);
        event.setLob(lob);
        event.setMasterName(masterName);
        event.setProgress(progress);
        event.setJobId(file.getJob().getId());
        kafkaTemplate.send(fileUpdatesTopic, fileId, event);

        FileProgressResponse response = new FileProgressResponse();
        response.setRequestId(event.getEventId());
        response.setStatus("ACCEPTED");
        response.setMessage("Progress update has been queued");
        response.setFileId(fileId);
        response.setMaster(masterName);
        response.setModeOfIntegration(progress.getModeOfIntegration());
        return ResponseEntity.accepted().body(response);
    }
} 