// File: service/FileService.java
package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.*;
import com.salescode.dis.insights.entity.*;
import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.exception.ResourceNotFoundException;
import com.salescode.dis.insights.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
public class FileService {

    private final FileRepository fileRepo;
    private final JobRepository  jobRepo;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public FileEntity register(Long jobId, FileRequest req) {
        JobEntity job = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));


        FileEntity file = FileEntity.builder()
                .fileId(req.getFileId())
                .source(req.getSource())
                .totalCount(req.getTotalCount())
                .publishedSuccessCount(req.getPublishedSuccess())
                .publishedFailCount(req.getPublishedFailure())
                .status(FileStatus.PENDING)
                .job(job)
                .build();

        job.getFiles().add(file);
        job.setTotalFileCount(job.getFiles().size());

        if (entityManager.contains(job)) {
            entityManager.merge(job);
        }

        log.info("Registered file {} under job {}", req.getFileId(), jobId);
        return fileRepo.save(file);
    }
    @Transactional(readOnly = true)
    public FileEntity get(String fileId) {
        return fileRepo.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    @Transactional
    public FileEntity updateProgress(String fileId, FileProgressRequest req) {
        FileEntity file = get(fileId);
        file.setConsumerSuccessCount(req.getConsumerSuccessCount());
        file.setConsumerFailCount(req.getConsumerFailCount());
        file.setPublishedSuccessCount(req.getPublishedSuccessCount());
        file.setPublishedFailCount(req.getPublishedFailCount());
        log.info("File {} progress: {}/{}", fileId,
                 req.getConsumerSuccessCount(), req.getConsumerFailCount());
        recalcJobMetrics(file.getJob());
        return fileRepo.save(file);
    }

    @Transactional
    public FileEntity updateStatus(String fileId, FileStatus status) {
        FileEntity file = get(fileId);
        file.setStatus(status);
        log.info("File {} status -> {}", fileId, status);
        recalcJobMetrics(file.getJob());
        return fileRepo.save(file);
    }

    private void recalcJobMetrics(JobEntity job) {
        long completed = job.getFiles().stream()
                .filter(f -> f.getStatus() == FileStatus.COMPLETED)
                .count();

        long failed = job.getFiles().stream()
                .filter(f -> f.getStatus() == FileStatus.FAILED)
                .count();

        job.setCompletedFiles((int) completed);
        job.setFailedFiles((int) failed);

        // Check if totalFileCount is null and provide a fallback value if necessary
        int totalFileCount = (job.getTotalFileCount() != null) ? job.getTotalFileCount() : 0;

        if (completed + failed == totalFileCount) {
            job.setStatus(failed > 0 ? JobStatus.FAILED : JobStatus.COMPLETED);
        }

        jobRepo.save(job);
        log.info("Job {} metrics: {}/{} completed, {} failed",
                job.getId(), completed, totalFileCount, failed);
    }


    public Optional<FileEntity> findById(String fileId){
        return fileRepo.findById(fileId);
    }

    public Page<FileResponse> getUnitsByJob(Long jobId, Pageable pageable) {
        Page<FileEntity> entities = fileRepo.findByJobId(jobId, pageable);
        return entities.map(this::convertToDto);
    }

    private FileResponse convertToDto(FileEntity entity) {
        return FileResponse.builder()
                .fileId(entity.getFileId())
                .source(entity.getSource())
                .totalCount(entity.getTotalCount())
                .publishedSuccessCount(entity.getPublishedSuccessCount())
                .publishedFailCount(entity.getPublishedFailCount())
                .consumedSuccessCount(entity.getConsumerSuccessCount())
                .consumedFailCount(entity.getConsumerFailCount())
                .status(entity.getStatus())
                .jobId(entity.getJob().getId())
                .build();

    }

}