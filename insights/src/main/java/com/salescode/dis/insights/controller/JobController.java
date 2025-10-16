package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.AccumulatedJobsAndMasterDto;
import com.salescode.dis.insights.dto.job.JobEntityRequestDto;
import com.salescode.dis.insights.dto.job.JobEntityResponseDto;
import com.salescode.dis.insights.dto.job.JobEntityResponseDtoWithStages;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ModeOfIntegration;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;

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

    @Operation(
        summary = "Create a new job",
        description = """
            Creates a new integration job.
            A job represents a unit of work that needs to be processed.
            
            The job will be created with a PENDING status by default.
            The job can be associated with publisher and consumer URIs for tracking purposes.
            Extended attributes can be provided for additional job metadata.
            """,
        parameters = {
            @Parameter(name = "lob", description = "Line of Business (Client)")
        }
    )
    @ApiResponse(
        responseCode = "201",
        description = "Job created successfully",
        content = @Content(schema = @Schema(implementation = JobEntityResponseDto.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request body or missing required fields",
        content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error occurred while creating the job",
        content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @PostMapping("/job")
    public ResponseEntity<JobEntityResponseDto> createJob(@PathVariable String lob, @Validated @RequestBody JobEntityRequestDto req) {
        JobEntity entity = jobEntityMapper.toEntity(req, lob);
        JobEntity job = jobService.saveJob(entity);
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Operation(summary = "Update an existing job", description = "Updates an existing job entity for the given LOB.")
    @ApiResponse(responseCode = "200", description = "Job updated successfully", content = @Content(schema = @Schema(implementation = JobEntityResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Job not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PutMapping("/job/update")
    public ResponseEntity<JobEntityResponseDto> updateJob(@PathVariable String lob,@Validated @RequestBody JobEntityRequestDto req) {
        JobEntity updatedJob = jobService.updateJob(req, lob);
        JobEntityResponseDto dto = jobEntityMapper.toDto(updatedJob);
        return ResponseEntity.ok(dto);
    }



    @Operation(
        summary = "Get job details",
        description = """
            Retrieves detailed information about a specific job by its ID.
            
            The response includes:
            - Job status and progress
            - Associated publisher and consumer URIs
            - Creation and modification timestamps
            - Extended attributes
            - File counts and completion status
            """,
        parameters = {
            @Parameter(name = "lob", description = "Line of Business"),
            @Parameter(name = "id", description = "Unique identifier of the job")
        }
    )
    @ApiResponse(
        responseCode = "200",
        description = "Job details retrieved successfully",
        content = @Content(schema = @Schema(implementation = JobEntityResponseDto.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Job not found with the specified ID",
        content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error occurred while retrieving job details",
        content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @GetMapping("/job/{id}")
    public ResponseEntity<JobEntityResponseDto> getJob(@PathVariable String lob, @PathVariable String id) {
        String decodedJobId = id;
        try{
            decodedJobId = new String(Base64.getDecoder().decode(id));
        } catch (Exception e) {}
        JobEntity job = jobService.getJob(decodedJobId);
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        return ResponseEntity.ok(dto);
    }



    @Operation(
            summary = "Update job status",
            description = """
        Updates the status of a job by its ID.

        The status must be one of the allowed values defined in the ProgressStatus enum.
        
        Example values include:
        - PENDING
        - RUNNING
        - COMPLETED_SUCCESSFULLY
        - COMPLETED_UNSUCCESSFULLY
        - FAILED
        - ABORTED
        """,
            parameters = {
                    @Parameter(name = "id", description = "Unique identifier of the job", required = true),
                    @Parameter(name = "status", description = "New status to be set for the job", required = true,
                            schema = @Schema(implementation = ProgressStatus.class))
            }
    )
    @ApiResponse(
            responseCode = "200",
            description = "Job status updated successfully",
            content = @Content(schema = @Schema(implementation = JobEntityResponseDto.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid status value or bad request",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Job not found with the specified ID",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Internal server error occurred while updating job status",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @PutMapping("/job/{id}/status/{status}")
    public ResponseEntity<JobEntityResponseDto> updateStatus(@PathVariable String id,@PathVariable ProgressStatus status){
        JobEntity job = jobService.updateStatus(id,status);
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        return ResponseEntity.ok(dto);
    }



    @Operation(
        summary = "List all jobs for a specific lob",
        description = """
            Retrieves a paginated list of all jobs for a specific line of business.
            
            The response can be paginated and sorted using standard Spring Data parameters:
            - page: Page number (0-based)
            - size: Number of items per page
            - sort: Field to sort by (e.g., startTime,asc)
            
            Results are sorted by start time in ascending order by default.
            """,
        parameters = {
            @Parameter(name = "lob", description = "Line of Business"),
            @Parameter(name = "page", description = "Page number (0-based)"),
            @Parameter(name = "size", description = "Number of items per page"),
            @Parameter(name = "sort", description = "Sort criteria (e.g., startTime,asc)")
        }
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of jobs retrieved successfully",
        content = @Content(schema = @Schema(implementation = JobEntityResponseDto.class))
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error occurred while retrieving job list",
        content = @Content(schema = @Schema(implementation = ApiError.class))
    )


    @GetMapping(path = "/jobs")
    public ResponseEntity<List<JobEntityResponseDtoWithStages>> getJobs(
            @PathVariable String lob,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false)  String mode) {

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
    public ResponseEntity<AccumulatedJobsAndMasterDto> getJobsAndMasters(
            @PathVariable String lob,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false)  String mode) {

        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(10);
        }

        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        AccumulatedJobsAndMasterDto result = jobService.getJobsWithAggregatedStagesAndMasters(lob, startDate, endDate, mode);
        return ResponseEntity.ok(result);
    }

    @GetMapping(path= "/modes")
    public ResponseEntity<Map<String,String>> getModes(@PathVariable String lob){
        Map<String,String> modes = jobService.getModesPerLob(lob);
        return ResponseEntity.ok(modes);
    }


    @Operation(
            summary = "Mark stale pending jobs as failed",
            description = """
        Finds all jobs for a specific LOB with PENDING status that haven't been 
        modified in the last hour and updates their status to FAILED.
        Returns a simple string message with the count of updated jobs.
        """)
    @ApiResponse(
            responseCode = "200",
            description = "Jobs successfully updated to FAILED status",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid LOB parameter",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Internal server error occurred",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @PutMapping(value = "/jobs/pending", produces = "text/plain")
    public ResponseEntity<String> timeoutPendingJobs(@PathVariable String lob) {

        if (lob == null || lob.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Error: LOB parameter is required");
        }

        try {
            // Calculate cutoff time (1 hour ago) using Instant
            Instant cutoffTime = Instant.now().minus(1, ChronoUnit.HOURS);

            log.info("Finding stale PENDING jobs for LOB {} older than {}", lob, cutoffTime);

            // Update the jobs to FAILED status
            int updatedCount = jobService.updateStaleJobs(
                    lob, ProgressStatus.PENDING, ProgressStatus.FAILED, cutoffTime);

            log.info("Updated {} stale PENDING jobs to FAILED for LOB {}", updatedCount, lob);

            // Return simple string message with count
            String message = String.format("Successfully updated %d stale PENDING jobs to FAILED status for LOB: %s",
                    updatedCount, lob);

            return ResponseEntity.ok(message);

        } catch (Exception e) {
            log.error("Error updating stale PENDING jobs for LOB {}: {}", lob, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: Failed to update stale jobs - " + e.getMessage());
        }
    }



}