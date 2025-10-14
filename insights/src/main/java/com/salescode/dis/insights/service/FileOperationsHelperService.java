package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.repository.FileStageMetricsRepository;
import com.salescode.dis.insights.service.strategy.IFileOperationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileOperationsHelperService {

    private final FileRepository fileRepository;
    private final FileStageMetricsRepository fileStageMetricsRepository;
    private final JobService jobService;

    @Transactional
    public FileEntity saveFileEntity(FileEntity fileEntity, JobEntity job, IFileOperationStrategy operationStrategy) {
        fileEntity.setJob(job);
        FileEntity savedFile = fileRepository.save(fileEntity);

        List<FileStageMetrics> list = operationStrategy.getSupportedStages()
                .stream()
                .sorted()
                .map(stage -> FileStageMetrics.builder()
                        .job(job)
                        .file(savedFile)
                        .lob(savedFile.getLob())
                        .master(fileEntity.getMaster())
                        .modeOfIntegration(fileEntity.getModeOfIntegration())
                        .stageType(stage)
                        .master(savedFile.getMaster())
                        .job(job)
                        .build())
                .map(build -> (FileStageMetrics) fileStageMetricsRepository.save(build))
                .toList();

        savedFile.setFileStageMetrics(list);
        return savedFile;
    }

    @Transactional
    public FileStageMetrics updateMetrics(FileEntity file, FileProgressRequest progress) {

        ProgressStage stageName = progress.getStageType();

        FileStageMetrics metrics = fileStageMetricsRepository.findByFileAndStageType(file, stageName)
//        FileStageMetrics metrics = file.getFileStageMetrics()
//                .stream()
//                .filter(m -> m.getStageType().equals(stageName))
//                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No metrics found for stage: " + stageName));

        metrics.setSuccessCount(metrics.getSuccessCount() + progress.getSuccessCount());
        metrics.setServerFailureCount(metrics.getServerFailureCount() + progress.getServerFailureCount());
        metrics.setLogicalFailureCount(metrics.getLogicalFailureCount() + progress.getLogicalFailureCount());

        if (progress.getMinProcessingTimeMs() != null) {
            metrics.setMinProcessingTimeMs(Optional.ofNullable(metrics.getMinProcessingTimeMs())
                    .map(currentMin -> Math.min(currentMin, progress.getMinProcessingTimeMs()))
                    .orElse(progress.getMinProcessingTimeMs()));
        }
        if (progress.getMaxProcessingTimeMs() != null) {
            metrics.setMaxProcessingTimeMs(Optional.ofNullable(metrics.getMaxProcessingTimeMs())
                    .map(currentMax -> Math.max(currentMax, progress.getMaxProcessingTimeMs()))
                    .orElse(progress.getMaxProcessingTimeMs()));
        }

        long totalRecordsForStage = metrics.getSuccessCount() + metrics.getServerFailureCount() + metrics.getLogicalFailureCount();

        if (metrics.getStartTime() != null) {
            long elapsedSeconds = Math.max(Duration.between(metrics.getStartTime(), Instant.now()).getSeconds(), 1);
            if (totalRecordsForStage > 0) {
                BigDecimal throughput = BigDecimal.valueOf(totalRecordsForStage).divide(BigDecimal.valueOf(elapsedSeconds), new MathContext(4));
                metrics.setThroughput(throughput);
            }
        }

        metrics.setProgressStatus(ProgressStatus.PENDING);
        if(!metrics.getFile().getStatus().equals(ProgressStatus.PENDING)){
            metrics.getFile().setStatus(ProgressStatus.PENDING);
            jobService.recalcStatus(metrics.getJob());
        }
        return fileStageMetricsRepository.save(metrics);
    }

}