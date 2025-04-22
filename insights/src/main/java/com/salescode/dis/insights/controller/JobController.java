package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.JobEntityRequestDto;
import com.salescode.dis.insights.dto.JobEntityResponseDto;
import com.salescode.dis.insights.dto.JobStatusUpdateRequestDto;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.mapper.JobEntityMapper;
import com.salescode.dis.insights.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/{lob}/master/{master_name}/job")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final JobService jobService;
    private final JobEntityMapper jobEntityMapper;

    @PostMapping
    public ResponseEntity<JobEntityResponseDto> createJob(@PathVariable String lob, @PathVariable("master_name") String master, @Validated @RequestBody JobEntityRequestDto req, UriComponentsBuilder uriBuilder) {
        JobEntity entity = jobEntityMapper.toEntity(req, lob, master);
        entity.setStatus(JobStatus.PENDING);
        JobEntity job = jobService.createJob(entity);
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        URI uri = uriBuilder.path("/api/{lob}/master/{master_name}/job/{id}")
                .buildAndExpand(lob, master, job.getId())
                .toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(uri).body(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobEntityResponseDto> getJob(@PathVariable String lob, @PathVariable("master_name") String master, @PathVariable String id) {
        JobEntity job = jobService.getJob(id);
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<JobEntityResponseDto> updateStatus(@PathVariable String lob, @PathVariable("master_name") String master, @PathVariable String id, @RequestBody JobStatusUpdateRequestDto statusReequest) {
        JobEntity job = jobService.updateStatus(id, JobStatus.valueOf(statusReequest.getStatus()));
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        return ResponseEntity.ok(dto);
    }
}