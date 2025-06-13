package com.salescode.dis.insights.service.strategy;

import com.salescode.dis.insights.dto.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.exception.ResourceNotFoundException;
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
public class FileBasedFileOperationStrategy implements IFileOperationStrategy {

    private final FileRepository fileRepository;
    private final JobService jobService;
    private final FileStageMetricsRepository fileStageMetricsRepository;
    private final FileStageMetricsUpdater fileStageMetricsUpdater;

    @Override
    @Transactional
    public FileEntity createFile(FileEntity fileEntity) {
        // Logic for file-based file creation
        JobEntity job = jobService.createJobIfNotExists(fileEntity.getJob().getId(), fileEntity.getLob());
        fileEntity.setJob(job);
        fileEntity.setModeOfIntegration(ModeOfIntegration.FILE_BASED);
        FileEntity savedFile = fileRepository.save(fileEntity);
        job.getFiles().add(savedFile);
        log.info("Registered file {} under job {} for FILE_BASED integration", savedFile.getId(), job.getId());
        return savedFile;
    }

    @Override
    @Transactional
    public void updateFileProgress(FileEntity fileEntity, String fileId, String masterName, String jobId, String lob, FileProgressRequest progress) {
        if (fileEntity == null) {
            // For FILE_BASED mode, files must already exist. Do not create on the fly.
            throw new ResourceNotFoundException("File not found: " + fileId + " for master: " + masterName + " in FILE_BASED mode. Files must be registered via API first.");
        }

        fileStageMetricsUpdater.updateMetrics(fileEntity, progress);
        log.info("FILE_BASED file {} progress updated for stage {}", fileEntity.getFileId(), progress.getStageName());
    }

    @Override
    public ModeOfIntegration getModeOfIntegration() {
        return ModeOfIntegration.FILE_BASED;
    }
} 