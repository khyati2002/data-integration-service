package com.salescode.dis.insights.service.strategy;

import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.repository.FileStageMetricsRepository;
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

    @Override
    @Transactional
    public FileEntity createFile(FileEntity fileEntity) {
        // Logic for API-based file creation
        JobEntity job = jobService.createJobIfNotExists(fileEntity.getJob().getId(), fileEntity.getLob());
        fileEntity.setJob(job);
        fileEntity.setModeOfIntegration(ModeOfIntegration.API_BASED);
        FileEntity savedFile = fileRepository.save(fileEntity);
        job.getFiles().add(savedFile);
        job.setTotalFileCount(job.getFiles().size());
        log.info("Registered file {} under job {} for API_BASED integration", savedFile.getId(), job.getId());
        return savedFile;
    }

    @Override
    @Transactional
    public void updateFileProgress(FileEntity fileEntity, String stageName, Long successCount, Long failureCount, Long minProcessingTimeMs, Long maxProcessingTimeMs) {
        // Logic for API-based file progress update
        FileStageMetrics metrics = fileEntity.getFileStageMetrics().stream()
                .filter(m -> m.getStageName().equals(stageName))
                .findFirst()
                .orElseGet(() -> {
                    FileStageMetrics newMetrics = new FileStageMetrics();
                    newMetrics.setFile(fileEntity);
                    newMetrics.setStageName(stageName); // Set the stageName here
                    fileEntity.getFileStageMetrics().add(newMetrics);
                    return newMetrics;
                });

        metrics.setSuccessCount(metrics.getSuccessCount() + successCount);
        metrics.setFailureCount(metrics.getFailureCount() + failureCount);

        if (minProcessingTimeMs != null) {
            metrics.setMinProcessingTimeMs(Optional.ofNullable(metrics.getMinProcessingTimeMs())
                    .map(currentMin -> Math.min(currentMin, minProcessingTimeMs))
                    .orElse(minProcessingTimeMs));
        }
        if (maxProcessingTimeMs != null) {
            metrics.setMaxProcessingTimeMs(Optional.ofNullable(metrics.getMaxProcessingTimeMs())
                    .map(currentMax -> Math.max(currentMax, maxProcessingTimeMs))
                    .orElse(maxProcessingTimeMs));
        }

        // Recalculate throughput for this stage based on its own start time
        long totalRecordsForStage = metrics.getSuccessCount() + metrics.getFailureCount();
        if (metrics.getStartTime() != null) { // Use stage's start time
            long elapsedSeconds = Math.max(Duration.between(metrics.getStartTime(), Instant.now()).getSeconds(), 1);
            if (totalRecordsForStage > 0) {
                // Increased precision for throughput calculation
                BigDecimal throughput = BigDecimal.valueOf(totalRecordsForStage).divide(BigDecimal.valueOf(elapsedSeconds), new MathContext(4));
                metrics.setThroughput(throughput);
            }
        }

        fileStageMetricsRepository.save(metrics);
        fileRepository.save(fileEntity);
        log.info("API_BASED file {} progress updated for stage {}", fileEntity.getFileId(), stageName);
    }

    @Override
    public ModeOfIntegration getModeOfIntegration() {
        return ModeOfIntegration.API_BASED;
    }
} 