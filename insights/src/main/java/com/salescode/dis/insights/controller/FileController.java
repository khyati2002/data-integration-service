package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.file.FileEntityRequestDto;
import com.salescode.dis.insights.dto.file.FileEntityResponseDto;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.mapped.TimeAwareEntity;
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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
    public ResponseEntity<FileEntityResponseDto> registerFile(@PathVariable String lob, @PathVariable("master_name") String masterName, @PathVariable String jobId, @Validated @RequestBody FileEntityRequestDto req) {
        FileEntity toSave = fileEntityMapper.toEntity(req, lob, masterName);
        FileEntity saved = fileService.createFile(jobId, toSave);
        FileEntityResponseDto resp = fileEntityMapper.toDto(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
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
        FileEntity file = fileService.get(decodedFileId, masterName);
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



}