package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.JobEntityRequestDto;
import com.salescode.dis.insights.dto.JobEntityResponseDto;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.entity.TimeAwareEntity;
import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.error.ApiError;
import com.salescode.dis.insights.mapper.JobEntityMapper;
import com.salescode.dis.insights.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/api/{lob}/master")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final JobService jobService;
    private final JobEntityMapper jobEntityMapper;

    @Operation(summary = "Create a new job", description = "Creates a new job entity under a specific master")
    @ApiResponse(responseCode = "201", description = "Job created successfully", content = @Content(schema = @Schema(implementation = JobEntityResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/{master_name}/job")
    public ResponseEntity<JobEntityResponseDto> createJob(@PathVariable String lob, @PathVariable("master_name") String master, @Validated @RequestBody JobEntityRequestDto req, UriComponentsBuilder uriBuilder) {
        JobEntity entity = jobEntityMapper.toEntity(req, lob, master);
        JobEntity job = jobService.createJob(entity);
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        URI uri = uriBuilder.path("/api/{lob}/master/{master_name}/job/{id}")
                .buildAndExpand(lob, master, job.getId())
                .toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(uri).body(dto);
    }

    @Operation(summary = "Get a specific job by ID", description = "Fetch a job by its unique ID")
    @ApiResponse(responseCode = "200", description = "Job found", content = @Content(schema = @Schema(implementation = JobEntityResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Job not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{master_name}/job/{id}")
    public ResponseEntity<JobEntityResponseDto> getJob(@PathVariable String lob, @PathVariable("master_name") String master, @PathVariable String id) {
        JobEntity job = jobService.getJob(id);
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Update job status", description = "Updates the status of an existing job")
    @ApiResponse(responseCode = "200", description = "Job status updated", content = @Content(schema = @Schema(implementation = JobEntityResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid status provided", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Job not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PutMapping("/{master_name}/job/{id}/status/{status}")
    public ResponseEntity<JobEntityResponseDto> updateStatus(@PathVariable String lob, @PathVariable("master_name") String master, @PathVariable String id, @PathVariable String status) {
        JobEntity job = jobService.updateStatus(id, JobStatus.valueOf(status));
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Get all jobs", description = "Retrieve a paginated list of all jobs for a specific line of business (LOB)")
    @ApiResponse(responseCode = "200", description = "List of jobs", content = @Content(schema = @Schema(implementation = JobEntityResponseDto.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping(path = "/jobs")
    public ResponseEntity<List<JobEntityResponseDto>> getJobs(@PathVariable String lob, Pageable pageable) {
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSortOr(Sort.by(Sort.Direction.ASC, TimeAwareEntity.START_TIME)));
        List<JobEntityResponseDto> content = jobService.getAllJobsByLob(lob, pageRequest)
                .map(jobEntityMapper::toDto)
                .getContent();
        return ResponseEntity.ok(content);
    }

    @Operation(summary = "Get jobs by master", description = "Retrieve a paginated list of jobs for a specific master in a specific line of business (LOB)")
    @ApiResponse(responseCode = "200", description = "List of jobs by master", content = @Content(schema = @Schema(implementation = JobEntityResponseDto.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping(path = "/{master_name}/jobs")
    public ResponseEntity<List<JobEntityResponseDto>> getJobsByMaster(@PathVariable String lob, @PathVariable("master_name") String master, Pageable pageable) {
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSortOr(Sort.by(Sort.Direction.ASC, TimeAwareEntity.START_TIME)));
        List<JobEntityResponseDto> content = jobService.getAllJobsByLobAndMaster(lob, master, pageRequest)
                .map(jobEntityMapper::toDto)
                .getContent();
        return ResponseEntity.ok(content);
    }

}