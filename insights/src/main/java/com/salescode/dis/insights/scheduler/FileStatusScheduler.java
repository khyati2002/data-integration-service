package com.salescode.dis.insights.scheduler;

import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.service.FileService;
import com.salescode.dis.insights.service.JobService;
import com.salescode.dis.insights.service.strategy.IFileOperationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.logging.ProgressBar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class FileStatusScheduler {

    // Define constants for time window
    @Value("${file-status-scheduler.stale-threshold-seconds:600}")
    public int STALE_THRESHOLD_SECONDS;

    @Value("${file-status-scheduler.too-old-threshold-seconds:900}")
    public int TOO_OLD_THRESHOLD_SECONDS;

    private final FileRepository fileRepository;
    private final JobService jobService;
    private final FileService fileService;


    @Scheduled(fixedRateString = "${file-status-scheduler.rate-millis:60000}") // Run every 1 minute (60000 ms)
    @Transactional
    public void updateAllFileStatus() {
        log.info("Starting scheduled update of all file statuses");

        // Define the time window for staleness
        Instant staleCutoffTime = Instant.now().minus(STALE_THRESHOLD_SECONDS, ChronoUnit.SECONDS);
        Instant tooOldCutoffTime = Instant.now().minus(TOO_OLD_THRESHOLD_SECONDS, ChronoUnit.SECONDS);

        // todo remove the check on status as PENDING
        List<FileEntity> pendingFiles = fileRepository.findAllStalePendingFiles(staleCutoffTime, tooOldCutoffTime, ProgressStatus.PENDING);

        if (pendingFiles.isEmpty()) {
            log.info("No stale files found in the {}-{} minute window.", STALE_THRESHOLD_SECONDS, TOO_OLD_THRESHOLD_SECONDS);
            return;
        }

        log.warn("Found {} potentially stale files (PENDING, modified between {}-{} mins ago). Marking as FAILED.", pendingFiles.size(), STALE_THRESHOLD_SECONDS, TOO_OLD_THRESHOLD_SECONDS);
        log.info("Found {} files with PENDING status modified in the last 10 minutes", pendingFiles.size());

        for (FileEntity file : pendingFiles) {
            updateFileStatus(file);
        }
    }

    private void updateFileStatus(FileEntity file) {
        IFileOperationStrategy strategy = fileService.getFileOperationStrategy(file.getModeOfIntegration());
        ProgressStage lastStage = strategy.getSupportedStages().getLast();
        ProgressStatus lastStageStatus = file.getFileStageMetrics()
                .stream()
                .filter(metric -> metric.getStageType() == lastStage)
                .findFirst()
                .map(FileStageMetrics::getProgressStatus)
                .orElse(ProgressStatus.PENDING);
        file.setStatus(lastStageStatus);
        file.setEndTime(Instant.now());
        jobService.recalcStatus(file.getJob());
        log.info("Updated file status and jobStatus " + file.getId());
    }
}
