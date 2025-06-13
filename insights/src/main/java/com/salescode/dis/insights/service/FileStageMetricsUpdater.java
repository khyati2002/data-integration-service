package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.repository.FileStageMetricsRepository;
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
public class FileStageMetricsUpdater {

    private final FileRepository fileRepository;
    private final FileStageMetricsRepository fileStageMetricsRepository;

    @Transactional
    public void updateMetrics(FileEntity file, FileProgressRequest progress) {
        String stageName = progress.getStageName();
        FileStageMetrics metrics = file.getFileStageMetrics().stream()
                .filter(m -> m.getStageName().equals(stageName))
                .findFirst()
                .orElseGet(() -> {
                    FileStageMetrics newMetrics = new FileStageMetrics();
                    newMetrics.setFile(file);
                    newMetrics.setStageName(stageName);
                    file.getFileStageMetrics().add(newMetrics);
                    return newMetrics;
                });

        metrics.setSuccessCount(metrics.getSuccessCount() + progress.getSuccessCount());
        metrics.setFailureCount(metrics.getFailureCount() + progress.getFailureCount());

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

        long totalRecordsForStage = metrics.getSuccessCount() + metrics.getFailureCount();
        if (metrics.getStartTime() != null) {
            long elapsedSeconds = Math.max(Duration.between(metrics.getStartTime(), Instant.now()).getSeconds(), 1);
            if (totalRecordsForStage > 0) {
                BigDecimal throughput = BigDecimal.valueOf(totalRecordsForStage).divide(BigDecimal.valueOf(elapsedSeconds), new MathContext(4));
                metrics.setThroughput(throughput);
            }
        }

        fileStageMetricsRepository.save(metrics);
        fileRepository.save(file);
        log.info("File {} progress updated for stage {}", file.getFileId(), stageName);
    }
} 