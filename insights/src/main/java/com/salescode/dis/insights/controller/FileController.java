package com.salescode.dis.insights.controller;


import com.salescode.dis.insights.dto.FileEntityRequestDto;
import com.salescode.dis.insights.dto.FileEntityResponseDto;
import com.salescode.dis.insights.dto.FileUpdateRequestDto;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/{lob}/master/{master_name}/job/{jobId}/unit")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;
    private final FileEntityMapper fileEntityMapper;


    @PostMapping
    public ResponseEntity<FileEntityResponseDto> registerFile(@PathVariable String lob, @PathVariable("master_name") String masterName, @PathVariable String jobId, @Validated @RequestBody FileEntityRequestDto req, UriComponentsBuilder uriBuilder) {
        FileEntity toSave = fileEntityMapper.toEntity(req);
        FileEntity saved = fileService.register(jobId, toSave);
        FileEntityResponseDto resp = fileEntityMapper.toDto(saved);
        URI uri = uriBuilder.path("/api/{lob}/master/{masterName}/job/{jobId}/unit/{id")
                .buildAndExpand(lob, masterName, jobId, saved.getId())
                .toUri();

        return ResponseEntity.status(HttpStatus.CREATED)
                .location(uri)
                .body(resp);

    }

    @GetMapping("/{fileId}")
    public ResponseEntity<FileEntityResponseDto> getFile(@PathVariable String lob, @PathVariable("master_name") String masterName, @PathVariable String fileId) {
        FileEntity file = fileService.get(fileId);
        FileEntityResponseDto resp = fileEntityMapper.toDto(file);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{fileId}/update")
    public ResponseEntity<FileEntityResponseDto> updateFile(@PathVariable String lob, @PathVariable("master_name") String masterName, @PathVariable String fileId, @Validated @RequestBody FileUpdateRequestDto req) {
        FileEntity updated;
        if (req.isProgressUpdate()) {
            updated = fileService.updateProgress(fileId, req.getProgress());
        } else if (req.isStatusUpdate()) {
            updated = fileService.updateStatus(fileId,req.getStatus().getConsumedStatus(), req.getStatus()
                    .getPublishedStatus());
        } else {
            return ResponseEntity.badRequest().build();
        }
        FileEntityResponseDto resp = fileEntityMapper.toDto(updated);
        return ResponseEntity.ok(resp);
    }

    @GetMapping
    public ResponseEntity<Page<FileEntityResponseDto>> listByJob(@PathVariable String lob, @PathVariable("master_name") String masterName, @PathVariable String jobId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<FileEntity> pageEnt = fileService.listByJob(jobId, PageRequest.of(page, size));
        Page<FileEntityResponseDto> pageDto = pageEnt.map(fileEntityMapper::toDto);
        return ResponseEntity.ok(pageDto);
    }
}