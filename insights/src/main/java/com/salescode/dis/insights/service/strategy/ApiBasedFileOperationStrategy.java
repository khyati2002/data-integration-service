package com.salescode.dis.insights.service.strategy;

import com.salescode.dis.insights.dto.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.repository.FileStageMetricsRepository;
import com.salescode.dis.insights.service.FileStageMetricsUpdater;
import com.salescode.dis.insights.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiBasedFileOperationStrategy implements IFileOperationStrategy {

    private final FileRepository fileRepository;
    private final JobService jobService;
    private final FileStageMetricsRepository fileStageMetricsRepository;
    private final FileStageMetricsUpdater fileStageMetricsUpdater;

    @Override
    @Transactional
    public FileEntity createFile(FileEntity fileEntity) {
        // Logic for API-based file creation
        JobEntity job = jobService.createJobIfNotExists(fileEntity.getJob().getId(), fileEntity.getLob());
        fileEntity.setJob(job);
        fileEntity.setModeOfIntegration(ModeOfIntegration.API_BASED);
        FileEntity savedFile = fileRepository.save(fileEntity);
        job.getFiles().add(savedFile);
        log.info("Registered file {} under job {} for API_BASED integration", savedFile.getId(), job.getId());
        return savedFile;
    }

    @Override
    @Transactional
    public void updateFileProgress(FileEntity fileEntity, String fileId, String masterName, String jobId, String lob, FileProgressRequest progress) {
        FileEntity file = fileEntity;
        if (file == null) {
            log.info("File not found for fileId: {}, master: {}. Creating new file and job if not exists.", fileId, masterName);
            // Create a new file along with the job if not exists
            JobEntity job = jobService.createJobIfNotExists(jobId, lob);

            file = new FileEntity();
            file.setFileId(fileId);
            file.setMaster(masterName);
            file.setLob(lob);
            file.setJob(job);
            file.setModeOfIntegration(ModeOfIntegration.API_BASED); // Always API_BASED for this strategy

            // Persist the newly created file entity
            file = fileRepository.save(file);
            job.getFiles().add(file); // Ensure job's file list is updated for totalFileCount on next job update
            log.info("Created new file entity (id: {}) for fileId: {}, master: {} under job: {}", file.getId(), fileId, masterName, jobId);
        }

        fileStageMetricsUpdater.updateMetrics(file, progress);
        log.info("API_BASED file {} progress updated for stage {}", file.getFileId(), progress.getStageName());
    }

    @Override
    public ModeOfIntegration getModeOfIntegration() {
        return ModeOfIntegration.API_BASED;
    }
} 