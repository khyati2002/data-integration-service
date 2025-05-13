package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.FileEntityRequestDto;
import com.salescode.dis.insights.dto.FileEntityResponseDto;
import com.salescode.dis.insights.dto.FileStatusRequestDto;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.TimeAwareEntity;
import com.salescode.dis.insights.exception.error.ApiError;
import com.salescode.dis.insights.mapper.FileEntityMapper;
import com.salescode.dis.insights.service.FileService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/{lob}")
@RequiredArgsConstructor
@Slf4j
public class FileController {
    private final FileService fileService;
    private final FileEntityMapper fileEntityMapper;

    @Operation(summary = "Register a new file", description = "Registers a new file entity for the job.")
    @ApiResponse(responseCode = "201", description = "File created successfully", content = @Content(schema = @Schema(implementation = FileEntityResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/master/{master_name}/job/{jobId}/unit")
    public ResponseEntity<FileEntityResponseDto> registerFile(@PathVariable String lob, @PathVariable("master_name") String masterName, @PathVariable String jobId, @Validated @RequestBody FileEntityRequestDto req, UriComponentsBuilder uriBuilder) {
        FileEntity toSave = fileEntityMapper.toEntity(req, lob, masterName);
        FileEntity saved = fileService.register(jobId, toSave);
        FileEntityResponseDto resp = fileEntityMapper.toDto(saved);
        URI uri = uriBuilder.path("/api/{lob}/master/{masterName}/job/{jobId}/unit/{id}")
                .buildAndExpand(lob, masterName, jobId, saved.getId())
                .toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(uri).body(resp);
    }

    @Operation(summary = "Get a specific file by ID", description = "Fetch a file by its unique ID.")
    @ApiResponse(responseCode = "200", description = "File found", content = @Content(schema = @Schema(implementation = FileEntityResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "File not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/master/{master_name}/job/{jobId}/unit/{fileId}")
    public ResponseEntity<FileEntityResponseDto> getFile(@PathVariable String lob, @PathVariable("master_name") String masterName, @PathVariable String fileId) {
        FileEntity file = fileService.get(fileId, masterName);
        FileEntityResponseDto resp = fileEntityMapper.toDto(file);
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Get all files for a specific job", description = "Retrieve a paginated list of all files associated with a specific job.")
    @ApiResponse(responseCode = "200", description = "List of files for the job", content = @Content(schema = @Schema(implementation = FileEntityResponseDto.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/job/{jobId}/unit")
    public ResponseEntity<List<FileEntityResponseDto>> listByJob(@PathVariable String lob, @PathVariable String jobId, Pageable pageable) {
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSortOr(Sort.by(Sort.Direction.ASC, TimeAwareEntity.START_TIME)));
        Page<FileEntity> pageEnt = fileService.listByJob(jobId, pageRequest);
        Page<FileEntityResponseDto> pageDto = pageEnt.map(fileEntityMapper::toDto);
        return ResponseEntity.ok(pageDto.getContent());
    }

    @Operation(summary = "Update file status", description = "Updates the status of a file")
    @ApiResponse(responseCode = "200", description = "Status updated successfully", content = @Content(schema = @Schema(implementation = FileEntityResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "File not found")
    @PutMapping("/master/{master_name}/job/{jobId}/unit/{fileId}/status")
    public ResponseEntity<FileEntityResponseDto> updateFileStatus(@PathVariable String lob, @PathVariable("master_name") String masterName, @PathVariable String jobId, @PathVariable String fileId, @Validated @RequestBody FileStatusRequestDto status) {
        if (!fileService.fileExists(fileId, masterName)) {
            return ResponseEntity.notFound().build();
        }
        FileEntity updatedFile = fileService.updateStatus(fileId, masterName, status);
        FileEntityResponseDto response = fileEntityMapper.toDto(updatedFile);
        return ResponseEntity.ok(response);
    }
}
