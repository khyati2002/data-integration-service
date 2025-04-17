package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.FileProgressRequest;
import com.salescode.dis.insights.dto.FileRequest;
import com.salescode.dis.insights.dto.FileResponse;
import com.salescode.dis.insights.dto.StatusUpdateRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.mapper.FileMapper; // Import the mapper
import com.salescode.dis.insights.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor // Use constructor injection
@Slf4j
public class FileController {

    private final FileService fileService;
    private final FileMapper fileMapper; // Inject the mapper

    @PostMapping("/job/{jobId}")
    public ResponseEntity<FileResponse> registerFile(@PathVariable Long jobId, @Validated @RequestBody FileRequest req) {
        FileEntity file = fileService.register(jobId, req); // Service likely uses FileRequest -> FileEntity mapping
        FileResponse resp = fileMapper.fileEntityToFileResponse(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<FileResponse> getFile(@PathVariable String fileId) {
        FileEntity file = fileService.get(fileId);
        FileResponse resp = fileMapper.fileEntityToFileResponse(file);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{fileId}/progress")
    public ResponseEntity<FileResponse> updateProgress(@PathVariable String fileId, @Validated @RequestBody FileProgressRequest req) {
        // Consider mapping FileProgressRequest to update the entity if needed in service
        FileEntity file = fileService.updateProgress(fileId, req);
        FileResponse resp = fileMapper.fileEntityToFileResponse(file);
        return ResponseEntity.ok(resp);
    }

    @PatchMapping("/{fileId}/status")
    public ResponseEntity<FileResponse> updateStatus(@PathVariable String fileId, @Validated @RequestBody StatusUpdateRequest req) {
        FileEntity file = fileService.updateStatus(fileId, FileStatus.valueOf(req.getStatus()));
        FileResponse resp = fileMapper.fileEntityToFileResponse(file);
        return ResponseEntity.ok(resp);
    }
}