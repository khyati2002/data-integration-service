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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileService {
    private final FileRepository fileRepo;
    private final JobRepository jobRepo;
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JobService jobService;

    public FileEntity register(String jobId, FileEntity file) {
        JobEntity job = jobRepo.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        file.setJob(job);
        file.setConsumedStatus(FileStatus.PENDING);
        file.setPublishedStatus(FileStatus.PENDING);
        if (file.getId() !=null && fileRepo.existsById(file.getId())) {
            throw new RuntimeException("File with ID " + file.getId() + " already exists");
        }
        else {
            FileEntity savedFile = fileRepo.save(file);
            job.getFiles().add(savedFile);
            job.setTotalFileCount(job.getFiles().size());
            jobRepo.save(job);
        }
        log.info("Registered file {} under job {}", file.getId(), jobId);
        return file;
    }

    public FileEntity registerOnUpdate(String jobId, FileEntity file,String master) {
        JobEntity job = jobRepo.findById(jobId).orElse(null);
        if(job == null){
            JobEntity jobEntity = new JobEntity();
            jobEntity.setId(jobId);
            jobEntity.setLob(file.getLob());
            jobEntity.setMaster(master);
            job = jobService.createJob(jobEntity);
        }
        file.setJob(job);
        file.setConsumedStatus(FileStatus.PENDING);
        file.setPublishedStatus(FileStatus.PENDING);

        FileEntity savedFile = fileRepo.save(file);
        job.getFiles().add(savedFile);
        job.setTotalFileCount(job.getFiles().size());
        jobRepo.save(job);

        log.info("Registered file {} under job {}", file.getId(), jobId);
        return file;
    }

    @Transactional(readOnly = true)
    public FileEntity get(String fileId) {
        return fileRepo.findById(fileId).orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    @Transactional(readOnly = true)
    public FileEntity getOrReturnNull(String fileId) {
        return fileRepo.findById(fileId).orElse(null);
    }

    public FileEntity updateProgress(String fileId, FileProgressRequest progress,String jobId, String lob,String master) {
        FileEntity file = getOrReturnNull(fileId);
        if(file == null){
            FileEntity fileEntity = new FileEntity();
            fileEntity.setId(fileId);
            fileEntity.setLob(lob);
            file = registerOnUpdate(jobId,fileEntity,master);
        }
        if(progress.getConsumerSuccessCount()!=null) {
            file.setConsumedSuccessCount(file.getConsumedSuccessCount() + progress.getConsumerSuccessCount());
        }
        if(progress.getConsumerFailCount()!=null) {
            file.setConsumedFailCount(file.getConsumedFailCount() + progress.getConsumerFailCount());
        }
        if(progress.getPublishedSuccessCount()!=null) {
            file.setPublishedSuccessCount(file.getPublishedSuccessCount() + progress.getPublishedSuccessCount());
        }
        if(progress.getPublishedFailCount()!=null) {
            file.setPublishedFailCount(file.getPublishedFailCount() + progress.getPublishedFailCount());
        }
        if(progress.getServerFailCount()!=null){
            file.setServerFailCount(file.getServerFailCount() + progress.getServerFailCount());
        }
        if(progress.getLogicalFailCount()!=null){
            file.setLogicalFailCount(file.getLogicalFailCount() + progress.getLogicalFailCount());
        }

        if(progress.getConsumerSuccessCount()!=null && progress.getConsumerFailCount()!=null && progress.getConsumerSuccessCount() + progress.getConsumerFailCount() == file.getTotalCount() ){
            file.setConsumedStatus(FileStatus.COMPLETED);
            file.setEndTime(Instant.now());
        }
        if(progress.getPublishedSuccessCount()!=null && progress.getPublishedFailCount()!=null && progress.getPublishedSuccessCount() + progress.getPublishedFailCount() == file.getTotalCount() ){
            file.setPublishedStatus(FileStatus.COMPLETED);
        }
        log.info("File {} progress updated", fileId);
        recalcJobMetrics(file.getJob());
        return fileRepo.save(file);
    }

    public FileEntity updateStatus(String fileId, FileStatus consumedStatus, FileStatus publishedStatus) {
        FileEntity file = get(fileId);
        file.setConsumedStatus(consumedStatus);
        file.setPublishedStatus(publishedStatus);
        log.info("File {} status -> {}", fileId, consumedStatus + "  " + publishedStatus);
        recalcJobMetrics(file.getJob());
        return fileRepo.save(file);
    }

    @Transactional(readOnly = true)
    public Page<FileEntity> listByJob(String jobId, Pageable pageable) {
        return fileRepo.findByJobId(jobId, pageable);
    }

    private void recalcJobMetrics(JobEntity job) {
        long completed = job.getFiles().stream().filter(f -> f.getConsumedStatus() == FileStatus.COMPLETED && f.getPublishedStatus() == FileStatus.COMPLETED).count();
        long failed = job.getFiles().stream().filter(f -> f.getConsumedStatus() == FileStatus.FAILED && f.getPublishedStatus() == FileStatus.FAILED).count();
        job.setCompletedFiles((int) completed);
        job.setFailedFiles((int) failed);
        if (completed + failed == job.getTotalFileCount()) {
            job.setStatus(failed > 0 ? JobStatus.FAILED : JobStatus.COMPLETED);
        }
        jobRepo.save(job);
        log.info("Job {} metrics recalculated", job.getId());
    }

    public boolean fileExists(String fileId) {
        return fileRepo.existsById(fileId);
    }
}