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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import com.salescode.dis.insights.enums.IntegrationMode;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileService {

    private final FileRepository fileRepo;
    private final JobService jobService;

    public FileEntity createFile(String jobId, FileEntity file) {
        checkFileIdAlreadyExistsByMasterIfSent(file);
        JobEntity job = jobService.getJob(jobId);
        file.setJob(job);
        FileEntity savedFile = fileRepo.save(file);
        job.getFiles().add(savedFile);
        job.setTotalFileCount(job.getFiles().size());
        log.info("Registered file {} under job {}", file.getId(), jobId);
        return savedFile;
    }

    private void checkFileIdAlreadyExistsByMasterIfSent(FileEntity file) {
        if (file.getFileId() != null && fileRepo.existsByFileIdAndMaster(file.getFileId(), Objects.requireNonNull(file.getMaster()))) {
            throw new IllegalArgumentException("File with fileId " + file.getFileId() + ", master " + file.getMaster() + " already exists");
        }
    }

    @Transactional(readOnly = true)
    public FileEntity get(String fileId, String master) {
        return fileRepo.findByFileIdAndMaster(fileId, master)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    @Transactional(readOnly = true)
    public Page<FileEntity> listByJob(String jobId, Pageable pageable) {
        return fileRepo.findByJobId(jobId, pageable);
    }

    public void updateProgress(String fileId, FileProgressRequest progress, String jobId, String lob, String masterName) {
        FileEntity file = fileRepo.findByFileIdAndMaster(fileId, masterName).orElseGet(() -> {
            FileEntity apiBasedFileEntity = new FileEntity();
            apiBasedFileEntity.setFileId(fileId);
            apiBasedFileEntity.setLob(lob);
            apiBasedFileEntity.setModeOfIntegration(IntegrationMode.API);
            return createJobIfNotExists(jobId, apiBasedFileEntity, masterName);
        });

        Optional.ofNullable(progress.getConsumer()).ifPresent(consumer -> {
            file.setConsumedSuccessCount(file.getConsumedSuccessCount() + consumer.getSuccessCount());
            file.setServerFailCount(file.getServerFailCount() + consumer.getServerFailCount());
            file.setLogicalFailCount(file.getLogicalFailCount() + consumer.getLogicalFailCount());
            file.setConsumedFailCount(file.getConsumedFailCount() + consumer.getServerFailCount() + consumer.getLogicalFailCount());
            file.setRetryCount(file.getRetryCount() + consumer.getRetryCount());
        });

        Optional.ofNullable(progress.getPublisher()).ifPresent(publisher -> {
            file.setPublishedSuccessCount(file.getPublishedSuccessCount() + publisher.getSuccessCount());
            file.setPublishedFailCount(file.getPublishedFailCount() + publisher.getFailCount());
            if (file.getModeOfIntegration().equals(IntegrationMode.API)) {
                file.setTotalCount(file.getTotalCount() + publisher.getSuccessCount() + publisher.getFailCount());
            }
        });

        if (progress.getMinProcessingTimeMs() != null) file.setMinProcessingTimeMs(file.getMinProcessingTimeMs() == null ? progress.getMinProcessingTimeMs() : Math.min(file.getMinProcessingTimeMs(), progress.getMinProcessingTimeMs()));
        if(progress.getMaxProcessingTimeMs() != null) file.setMaxProcessingTimeMs(file.getMaxProcessingTimeMs() == null ? progress.getMaxProcessingTimeMs() : Math.max(file.getMaxProcessingTimeMs(), progress.getMaxProcessingTimeMs()));

        fileRepo.save(file);
        log.info("File {} progress updated", fileId);
    }

    protected FileEntity createJobIfNotExists(String jobId, FileEntity file, String master) {
        JobEntity job = jobService.createJobIfNotExists(jobId, file.getLob());
        file.setJob(job);
        file.setMaster(master);
        FileEntity savedFile = fileRepo.save(file);
        job.getFiles().add(savedFile);
        job.setTotalFileCount(job.getFiles().size());
        log.info("Registered new file {} under job {}", file.getId(), jobId);
        return savedFile;
    }

    public FileEntity updateStatus(String fileId, String masterName, FileStatusRequestDto status) {
        FileEntity file = get(fileId, masterName);
        if (status.getConsumedStatus() != null) {
            file.setConsumedStatus(status.getConsumedStatus());
        }
        if (status.getPublishedStatus() != null) {
            file.setPublishedStatus(status.getPublishedStatus());
        }
        log.info("File {} status updated to consumed: {}, published: {}", fileId, status.getConsumedStatus(), status.getPublishedStatus());
        recalcJobMetrics(file.getJob());
        return fileRepo.save(file);
    }

    protected void recalcJobMetrics(JobEntity job) {
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
        if ((completed != 0 || failed != 0) && completed + failed == job.getTotalFileCount()) {
            job.setStatus(failed > 0 ? JobStatus.FAILED : JobStatus.COMPLETED);
        }
        log.info("Job {} metrics recalculated", job.getId());
    }


}