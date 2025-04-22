package com.salescode.dis.insights.service;

import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.exception.ResourceNotFoundException;
import com.salescode.dis.insights.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepo;

    @Transactional
    public JobEntity createJob(JobEntity req) {
        JobEntity saved = jobRepo.save(req);
        log.info("Created job {}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public JobEntity getJob(String id) {
        return jobRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));
    }

    @Transactional
    public JobEntity updateStatus(String id, JobStatus status) {
        JobEntity job = getJob(id);
        job.setStatus(status);
        log.info("Job {} status -> {}", id, status);
        return job;
    }

    @Transactional(readOnly = true)
    public Page<JobEntity> getAllJobsByLob(String lob, Pageable pageable) {
        return jobRepo.getJobEntitiesByLob(lob, pageable);
    }
}