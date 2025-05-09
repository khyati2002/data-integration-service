// File: service/FileService.java
package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.FileProgressRequest;
import com.salescode.dis.insights.dto.FileStatusRequestDto;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.exception.ResourceNotFoundException;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileService {

    private final FileRepository fileRepo;
    private final JobRepository jobRepo;
    private final JobService jobService;

    public FileEntity register(String jobId, FileEntity file) {
        JobEntity job = jobService.getJob(jobId);
        file.setJob(job);
        if (file.getId() != null && fileRepo.existsById(file.getId())) {
            throw new IllegalArgumentException("File with ID " + file.getId() + " already exists");
        } else {
            FileEntity savedFile = fileRepo.save(file);
            job.getFiles().add(savedFile);
            job.setTotalFileCount(job.getFiles().size());
            jobRepo.save(job);
        }
        log.info("Registered file {} under job {}", file.getId(), jobId);
        return file;
    }

    public FileEntity registerOnUpdate(String jobId, FileEntity file, String master) {
        JobEntity job = jobRepo.findById(jobId).orElseGet(() -> {
            JobEntity jobEntity = new JobEntity();
            jobEntity.setId(jobId);
            jobEntity.setLob(file.getLob());
    //        jobEntity.setMaster(master);
            return jobService.createJob(jobEntity);
        });
        file.setJob(job);
        file.setMaster(master);
        FileEntity savedFile = fileRepo.save(file);
        job.getFiles().add(savedFile);
        job.setTotalFileCount(job.getFiles().size());
        jobRepo.save(job);
        log.info("Registered new file {} under job {}", file.getId(), jobId);
        return savedFile;
    }

    @Transactional(readOnly = true)
    public FileEntity get(String fileId, String master) {
        return fileRepo.findByFileIdAndMaster(fileId,master).orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    @Transactional(readOnly = true)
    public FileEntity getOrReturnNull(String fileId, String master) {
        return fileRepo.findByFileIdAndMaster(fileId,master).orElse(null);
    }



    public void updateProgress(String fileId, FileProgressRequest progress, String jobId, String lob, String masterName) {
        // todo fix for concurrent updates on multiple consumers
        FileEntity file = getOrReturnNull(fileId,masterName);
        if (file == null) {
            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileId(fileId);
            fileEntity.setLob(lob);
            fileEntity.setIsApiBased(true);
            file = registerOnUpdate(jobId, fileEntity, masterName);
        }

        if (progress.getConsumer() != null) {
            file.setConsumedSuccessCount(file.getConsumedSuccessCount() + progress.getConsumer().getSuccessCount());
            file.setServerFailCount(file.getServerFailCount() + progress.getConsumer().getServerFailCount());
            file.setLogicalFailCount(file.getLogicalFailCount() + progress.getConsumer().getLogicalFailCount());
            file.setConsumedFailCount(
                file.getConsumedFailCount() + progress.getConsumer().getServerFailCount() + progress.getConsumer().getLogicalFailCount()
            );
        }

        if (progress.getPublisher() != null) {
            file.setPublishedSuccessCount(file.getPublishedSuccessCount() + progress.getPublisher().getSuccessCount());
            file.setPublishedFailCount(file.getPublishedFailCount() + progress.getPublisher().getFailCount());
            if(file.getIsApiBased()){
                file.setTotalCount(file.getTotalCount() + progress.getPublisher().getSuccessCount() + progress.getPublisher().getFailCount());
            }
        }

        fileRepo.save(file);
        log.info("File {} progress updated", fileId);
    }

    public FileEntity updateStatus(String fileId, String masterName, FileStatusRequestDto status) {
        FileEntity file = getOrReturnNull(fileId,masterName);
        if (status.getConsumedStatus() != null) {
            file.setConsumedStatus(status.getConsumedStatus());
        }
        if (status.getPublishedStatus() != null) {
            file.setPublishedStatus(status.getPublishedStatus());
        }
        log.info("File {} status updated to consumed: {}, published: {}",fileId, status.getConsumedStatus(), status.getPublishedStatus());
        recalcJobMetrics(file.getJob());
//        return fileRepo.save(file);
        return file;
    }

    @Transactional(readOnly = true)
    public Page<FileEntity> listByJob(String jobId, Pageable pageable) {
        return fileRepo.findByJobId(jobId, pageable);
    }

    public void recalcJobMetrics(JobEntity job) {
        long completed = job.getFiles()
                .stream()
                .filter(f -> f.getConsumedStatus() == FileStatus.COMPLETED && f.getPublishedStatus() == FileStatus.COMPLETED)
                .count();
        long failed = job.getFiles()
                .stream()
                .filter(f -> f.getConsumedStatus() == FileStatus.FAILED && f.getPublishedStatus() == FileStatus.FAILED)
                .count();
        job.setCompletedFiles((int) completed);
        job.setFailedFiles((int) failed);
        if ((completed!=0  || failed!=0) && completed + failed == job.getTotalFileCount()) {
            job.setStatus(failed > 0 ? JobStatus.FAILED : JobStatus.COMPLETED);
        }
        jobRepo.save(job);
        log.info("Job {} metrics recalculated", job.getId());
    }

    public boolean fileExists(String fileId, String masterName) {
        return fileRepo.existsByFileIdAndMaster(fileId,masterName);
    }
}