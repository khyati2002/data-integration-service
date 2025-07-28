package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.AccumulatedJobsAndMasterDto;
import com.salescode.dis.insights.dto.job.JobEntityRequestDto;
import com.salescode.dis.insights.dto.job.JobEntityResponseDto;
import com.salescode.dis.insights.dto.job.JobEntityResponseDtoWithStages;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.exception.error.ApiError;
import com.salescode.dis.insights.mapper.FileEntityMapper;
import com.salescode.dis.insights.mapper.JobEntityMapper;
import com.salescode.dis.insights.repository.FileStageMetricsRepository;
import com.salescode.dis.insights.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/{lob}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Job Management", description = "APIs for managing integration jobs")
public class JobController {
    private final FileStageMetricsRepository fileStageMetricsRepository;

    private final JobService jobService;
    private final JobEntityMapper jobEntityMapper;
    private final FileEntityMapper fileEntityMapper;

    @Operation(summary = "Create a new job", parameters = {@Parameter(name = "lob", description = "Line of Business (Client)")})
    @ApiResponse(responseCode = "201", description = "Job created successfully", content = @Content(schema = @Schema(implementation = JobEntityResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request body or missing required fields", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error occurred while creating the job", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/job")
    public ResponseEntity<JobEntityResponseDto> createJob(@PathVariable String lob, @Validated @RequestBody JobEntityRequestDto req) {
        JobEntity entity = jobEntityMapper.toEntity(req, lob);
        JobEntity job = jobService.saveJob(entity);
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }


    @Operation(summary = "Get job details", parameters = {@Parameter(name = "lob", description = "Line of Business"), @Parameter(name = "id", description = "Unique identifier of the job")})
    @ApiResponse(responseCode = "200", description = "Job details retrieved successfully", content = @Content(schema = @Schema(implementation = JobEntityResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Job not found with the specified ID", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error occurred while retrieving job details", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/job/{id}")
    public ResponseEntity<JobEntityResponseDto> getJob(@PathVariable String lob, @PathVariable String id) {
        String decodedJobId = id;
        try {
            decodedJobId = new String(Base64.getDecoder().decode(id));
        } catch (Exception e) {
        }
        JobEntity job = jobService.getJob(decodedJobId);
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        return ResponseEntity.ok(dto);
    }


    @Operation(summary = "Update job status", parameters = {@Parameter(name = "id", description = "Unique identifier of the job", required = true), @Parameter(name = "status", description = "New status to be set for the job", required = true, schema = @Schema(implementation = ProgressStatus.class))})
    @ApiResponse(responseCode = "200", description = "Job status updated successfully", content = @Content(schema = @Schema(implementation = JobEntityResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid status value or bad request", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Job not found with the specified ID", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error occurred while updating job status", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PutMapping("/job/{id}/status/{status}")
    public ResponseEntity<JobEntityResponseDto> updateStatus(@PathVariable String id, @PathVariable ProgressStatus status) {
        JobEntity job = jobService.updateStatus(id, status);
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        return ResponseEntity.ok(dto);
    }


    @Operation(summary = "List all jobs for a specific lob", parameters = {@Parameter(name = "lob", description = "Line of Business"), @Parameter(name = "page", description = "Page number (0-based)"), @Parameter(name = "size", description = "Number of items per page"), @Parameter(name = "sort", description = "Sort criteria (e.g., startTime,asc)")})
    @ApiResponse(responseCode = "200", description = "List of jobs retrieved successfully", content = @Content(schema = @Schema(implementation = JobEntityResponseDto.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error occurred while retrieving job list", content = @Content(schema = @Schema(implementation = ApiError.class)))


    @GetMapping(path = "/jobs")
    public ResponseEntity<List<JobEntityResponseDtoWithStages>> getJobs(@PathVariable String lob, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate, @RequestParam(required = false) String mode) {

        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(1);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        List<JobEntityResponseDtoWithStages> result = jobService.getJobsWithAggregatedStages(lob, startDate, endDate, mode);
        return ResponseEntity.ok(result);
    }

    @GetMapping(path = "/all-jobs")
    public ResponseEntity<AccumulatedJobsAndMasterDto> getJobsAndMasters(@PathVariable String lob, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate, @RequestParam(required = false) String mode) {

        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(10);
        }

        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        AccumulatedJobsAndMasterDto result = jobService.getJobsWithAggregatedStagesAndMasters(lob, startDate, endDate, mode);
        return ResponseEntity.ok(result);
    }


}