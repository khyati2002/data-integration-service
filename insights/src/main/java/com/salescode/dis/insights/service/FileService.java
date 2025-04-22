// File: service/FileService.java
package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.exception.ResourceNotFoundException;
import com.salescode.dis.insights.repository.FileRepository;
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
@Transactional
public class FileService {
    private final FileRepository fileRepo;
    private final JobRepository jobRepo;

    public FileEntity register(String jobId, FileEntity file) {
        JobEntity job = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        file.setJob(job);
        job.getFiles().add(file);
        job.setTotalFileCount(job.getFiles().size());
        // cascade saves file
        jobRepo.save(job);
        log.info("Registered file {} under job {}", file.getId(), jobId);
        return file;
    }

    @Transactional(readOnly = true)
    public FileEntity get(String fileId) {
        return fileRepo.findById(fileId).orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    public FileEntity updateProgress(String fileId, FileProgressRequest progress) {
        FileEntity file = get(fileId);
        file.setConsumedSuccessCount(progress.getConsumerSuccessCount());
        file.setConsumedFailCount(progress.getConsumerFailCount());
        file.setPublishedSuccessCount(progress.getPublishedSuccessCount());
        file.setPublishedFailCount(progress.getPublishedFailCount());
        log.info("File {} progress updated", fileId);
        recalcJobMetrics(file.getJob());
        return fileRepo.save(file);
    }

    public FileEntity updateStatus(String fileId, FileStatus status) {
        FileEntity file = get(fileId);
        file.setStatus(status);
        log.info("File {} status -> {}", fileId, status);
        recalcJobMetrics(file.getJob());
        return fileRepo.save(file);
    }

    @Transactional(readOnly = true)
    public Page<FileEntity> listByJob(Long jobId, Pageable pageable) {
        return fileRepo.findByJobId(jobId, pageable);
    }

    private void recalcJobMetrics(JobEntity job) {
        long completed = job.getFiles().stream().filter(f -> f.getStatus() == FileStatus.COMPLETED).count();
        long failed = job.getFiles().stream().filter(f -> f.getStatus() == FileStatus.FAILED).count();
        job.setCompletedFiles((int) completed);
        job.setFailedFiles((int) failed);
        if (completed + failed == job.getTotalFileCount()) {
            job.setStatus(failed > 0 ? JobStatus.FAILED : JobStatus.COMPLETED);
        }
        jobRepo.save(job);
        log.info("Job {} metrics recalculated", job.getId());
    }
}