package com.salescode.dis.insights.service;

import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.exception.ResourceNotFoundException;
import com.salescode.dis.insights.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class JobService {

    private final JobRepository jobRepo;

    public JobEntity saveJob(JobEntity req) {
        return jobRepo.save(req);
    }

    @Transactional(readOnly = true)
    public JobEntity getJob(String id) {
        return jobRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
    }

/*    public JobEntity updateStatus(String id, ProgressStatus status) {
        JobEntity job = getJob(id);
        job.setStatus(status);
        log.info("Job {} status -> {}", id, status);
        return job;
    }*/

    @Transactional(readOnly = true)
    public Page<JobEntity> getAllJobsByLob(String lob, Pageable pageable) {
        return jobRepo.getJobEntitiesByLob(lob, pageable);
    }

    public JobEntity createJobIfNotExists(String jobId, String lob, ModeOfIntegration modeOfIntegration) {
        Optional<JobEntity> job = jobRepo.findById(jobId);
        return job.orElseGet(()->{
            JobEntity jobEntity = new JobEntity();
            jobEntity.setId(jobId);
            jobEntity.setLob(lob);
            jobEntity.setModeOfIntegration(modeOfIntegration);
            return saveJob(jobEntity);
        });
    }
}