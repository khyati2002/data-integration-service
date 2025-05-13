package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.JobEntityRequestDto;
import com.salescode.dis.insights.dto.JobEntityResponseDto;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.entity.TimeAwareEntity;
import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.exception.error.ApiError;
import com.salescode.dis.insights.mapper.JobEntityMapper;
import com.salescode.dis.insights.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
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

@RestController
@RequestMapping("/api/{lob}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Job Management", description = "APIs for managing integration jobs")
public class JobController {

    private final JobService jobService;
    private final JobEntityMapper jobEntityMapper;



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
    public ResponseEntity<JobEntityResponseDto> createJob(@PathVariable String lob, @Validated @RequestBody JobEntityRequestDto req, UriComponentsBuilder uriBuilder) {
        JobEntity entity = jobEntityMapper.toEntity(req, lob);
        JobEntity job = jobService.saveJob(entity);
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        URI uri = uriBuilder.path("/api/{lob}/job/{id}")
                .buildAndExpand(lob, job.getId())
                .toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(uri).body(dto);
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
        JobEntity job = jobService.getJob(id);
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        return ResponseEntity.ok(dto);
    }




    @Operation(
        summary = "Update job status",
        description = """
            Updates the status of an existing job.
            
            Valid status values are:
            - PENDING: Initial state when job is created
            - RUNNING: Job is currently being processed
            - COMPLETED: Job has finished successfully
            - FAILED: Job has failed during processing
            - CANCELLED: Job was cancelled before completion
            """,
        parameters = {
            @Parameter(name = "lob", description = "Line of Business"),
            @Parameter(name = "id", description = "Unique identifier of the job"),
            @Parameter(name = "status", description = "New status to set for the job")
        }
    )
    @ApiResponse(
            responseCode = "200",
            description = "Status update request accepted and procesed successfully",
            content = @Content(schema = @Schema(implementation = JobEntityResponseDto.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid status value provided",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Job not found with the specified ID",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Internal server error occurred while processing status update",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @PutMapping("/job/{id}/status/{status}")
    public ResponseEntity<JobEntityResponseDto> updateStatus(@PathVariable String lob, @PathVariable String id, @PathVariable @NotBlank JobStatus status) {
        JobEntity job = jobService.updateStatus(id, status);
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
    public ResponseEntity<Page<JobEntityResponseDto>> getJobs(@PathVariable String lob, Pageable pageable) {
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSortOr(Sort.by(Sort.Direction.DESC, TimeAwareEntity.START_TIME)));
        Page<JobEntityResponseDto> content = jobService.getAllJobsByLob(lob, pageRequest).map(jobEntityMapper::toDto);
        return ResponseEntity.ok(content);
    }


}