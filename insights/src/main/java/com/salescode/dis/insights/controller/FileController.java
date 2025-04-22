package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.FileEntityDto;
import com.salescode.dis.insights.dto.FileUpdateRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.mapper.FileEntityMapper;
import com.salescode.dis.insights.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/{lob}/master/{masterName}/job/{jobId}/unit")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;
    private final FileEntityMapper fileEntityMapper;

    @PostMapping
    public ResponseEntity<FileEntityDto> registerFile(@PathVariable String lob, @PathVariable String masterName, @PathVariable String jobId, @Validated @RequestBody FileEntityDto req) {
        FileEntity toSave = fileEntityMapper.toEntity(req);
        FileEntity saved = fileService.register(jobId, toSave);
        FileEntityDto resp = fileEntityMapper.toDto(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<FileEntityDto> getFile(@PathVariable String lob, @PathVariable String masterName, @PathVariable String fileId) {
        FileEntity file = fileService.get(fileId);
        FileEntityDto resp = fileEntityMapper.toDto(file);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{fileId}/update")
    public ResponseEntity<FileEntityDto> updateFile(@PathVariable String lob, @PathVariable String masterName, @PathVariable String fileId, @Validated @RequestBody FileUpdateRequest req) {
        FileEntity updated;
        if (req.isProgressUpdate()) {
            updated = fileService.updateProgress(fileId, req.getProgress());
        } else if (req.isStatusUpdate()) {
            updated = fileService.updateStatus(fileId, FileStatus.valueOf(req.getStatus()));
        } else {
            return ResponseEntity.badRequest().build();
        }
        FileEntityDto resp = fileEntityMapper.toDto(updated);
        return ResponseEntity.ok(resp);
    }

    @GetMapping
    public ResponseEntity<Page<FileEntityDto>> listByJob(@PathVariable String lob, @PathVariable String masterName, @PathVariable Long jobId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<FileEntity> pageEnt = fileService.listByJob(jobId, PageRequest.of(page, size));
        Page<FileEntityDto> pageDto = pageEnt.map(fileEntityMapper::toDto);
        return ResponseEntity.ok(pageDto);
    }
}