package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.JobEntityRequestDto;
import com.salescode.dis.insights.dto.JobEntityResponseDto;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.entity.TimeAwareEntity;
import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.mapper.JobEntityMapper;
import com.salescode.dis.insights.service.JobService;
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

    @GetMapping("/{master_name}/job/{id}")
    public ResponseEntity<JobEntityResponseDto> getJob(@PathVariable String lob, @PathVariable("master_name") String master, @PathVariable String id) {
        JobEntity job = jobService.getJob(id);
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{master_name}/job/{id}/status/{status}")
    public ResponseEntity<JobEntityResponseDto> updateStatus(@PathVariable String lob, @PathVariable("master_name") String master, @PathVariable String id, @PathVariable String status) {
        JobEntity job = jobService.updateStatus(id, JobStatus.valueOf(status));
        JobEntityResponseDto dto = jobEntityMapper.toDto(job);
        return ResponseEntity.ok(dto);
    }

    @GetMapping(path = "/jobs")
    public ResponseEntity<List<JobEntityResponseDto>> getJobs(@PathVariable String lob, Pageable pageable) {
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSortOr(Sort.by(Sort.Direction.ASC, TimeAwareEntity.START_TIME)));
        List<JobEntityResponseDto> content = jobService.getAllJobsByLob(lob, pageRequest)
                .map(jobEntityMapper::toDto)
                .getContent();
        return ResponseEntity.ok(content);
    }

    @GetMapping(path = "/{master}/jobs")
    public ResponseEntity<List<JobEntityResponseDto>> getJobsByMaster(@PathVariable String lob, @PathVariable String master, Pageable pageable) {
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSortOr(Sort.by(Sort.Direction.ASC, TimeAwareEntity.START_TIME)));
        List<JobEntityResponseDto> content = jobService.getAllJobsByLobAndMaster(lob, master, pageRequest)
                .map(jobEntityMapper::toDto)
                .getContent();
        return ResponseEntity.ok(content);
    }

}