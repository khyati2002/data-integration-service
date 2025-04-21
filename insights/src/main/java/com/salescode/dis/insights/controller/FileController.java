package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.*;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.service.FileService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/{lob}/master/{master-name}/job/{job_id}/unit")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;

    @PostMapping
    public ResponseEntity<FileResponse> registerFile(@PathVariable("lob") String lob,
                                                     @PathVariable("master-name") String masterName,
                                                     @PathVariable("job_id") Long jobId,
                                                     @Validated @RequestBody FileRequest req) {


        FileEntity file = fileService.register(jobId, req);
        FileResponse resp = FileResponse.builder()
                .fileId(file.getFileId())
                .source(file.getSource())
                .totalCount(file.getTotalCount())
                .publishedSuccessCount(file.getPublishedSuccessCount())
                .publishedFailCount(file.getPublishedFailCount())
                .consumedSuccessCount(file.getConsumerSuccessCount())
                .consumedFailCount(file.getConsumerFailCount())
                .status(file.getStatus())
                .jobId(jobId)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<FileResponse> getFile(@PathVariable String fileId,
                                                @PathVariable("lob") String lob,
                                                @PathVariable("master-name") String masterName) {
        FileEntity file = fileService.get(fileId);
        FileResponse resp = FileResponse.builder()
                .fileId(file.getFileId())
                .source(file.getSource())
                .totalCount(file.getTotalCount())
                .publishedSuccessCount(file.getPublishedSuccessCount())
                .publishedFailCount(file.getPublishedFailCount())
                .consumedSuccessCount(file.getConsumerSuccessCount())
                .consumedFailCount(file.getConsumerFailCount())
                .status(file.getStatus())
                .jobId(file.getJob().getId())
                .build();

        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{fileId}/update")
    public ResponseEntity<FileResponse> updateFile(
            @PathVariable String fileId,
            @PathVariable("lob") String lob,
            @PathVariable("master-name") String masterName,
            @Validated @RequestBody FileUpdateRequest req) {

        FileEntity file = null;

        // Check if progress is provided, update progress
        if (req.isProgressUpdate()) {
            file = fileService.updateProgress(fileId, req.getProgress());
        }
        // Check if status is provided, update status
        if (req.isStatusUpdate()) {
            file = fileService.updateStatus(fileId, FileStatus.valueOf(req.getStatus()));
        }

        if (!req.isProgressUpdate() && !req.isStatusUpdate()) {
            return ResponseEntity.badRequest().build();
        }

        FileResponse resp = FileResponse.builder()
                .fileId(file.getFileId())
                .source(file.getSource())
                .totalCount(file.getTotalCount())
                .publishedSuccessCount(file.getPublishedSuccessCount())
                .publishedFailCount(file.getPublishedFailCount())
                .consumedSuccessCount(file.getConsumerSuccessCount())
                .consumedFailCount(file.getConsumerFailCount())
                .status(file.getStatus())
                .jobId(file.getJob().getId())
                .build();

        return ResponseEntity.ok(resp);
    }

    @GetMapping
    public ResponseEntity<Page<FileResponse>> getUnitsByJob(
            @PathVariable String lob,
            @PathVariable("master-name") String masterName,
            @PathVariable("job_id") Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<FileResponse> units = fileService.getUnitsByJob(jobId, PageRequest.of(page, size));
        return ResponseEntity.ok(units);
    }


}