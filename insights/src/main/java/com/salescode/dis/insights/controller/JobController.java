package com.salescode.dis.insights.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dis.insights.dto.JobRequest;
import com.salescode.dis.insights.dto.JobResponse;
import com.salescode.dis.insights.dto.StatusUpdateRequest;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/{lob}/master/{master_name}/job")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @PathVariable String lob,
            @PathVariable("master_name") String master,
            @Validated @RequestBody JobRequest req) {

        JobEntity job = jobService.createJob(req);
        JobResponse resp = JobResponse.builder()
                .id(job.getId())
                .lob(job.getLob())
                .master(job.getMaster())
                .status(job.getStatus())
                .totalFileCount(job.getTotalFileCount())
                .completedFiles(job.getCompletedFiles())
                .failedFiles(job.getFailedFiles())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(resp);

    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(
            @PathVariable String lob,
            @PathVariable("master_name") String master,
            @PathVariable Long id) {

        JobEntity job = jobService.getJob(id);
        JobResponse resp = JobResponse.builder()
                .id(job.getId())
                .lob(job.getLob())
                .master(job.getMaster())
                .status(job.getStatus())
                .totalFileCount(job.getTotalFileCount())
                .completedFiles(job.getCompletedFiles())
                .failedFiles(job.getFailedFiles())
                .build();
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<JobResponse> updateStatus(
            @PathVariable String lob,
            @PathVariable("master_name") String master,
            @PathVariable Long id,
            @Validated @RequestBody StatusUpdateRequest req) {

        JobEntity job = jobService.updateStatus(id, JobStatus.valueOf(req.getStatus()));
        JobResponse resp = JobResponse.builder()
                .id(job.getId())
                .lob(job.getLob())
                .master(job.getMaster())
                .status(job.getStatus())
                .totalFileCount(job.getTotalFileCount())
                .completedFiles(job.getCompletedFiles())
                .failedFiles(job.getFailedFiles())
                .build();
        return ResponseEntity.ok(resp);
    }
}