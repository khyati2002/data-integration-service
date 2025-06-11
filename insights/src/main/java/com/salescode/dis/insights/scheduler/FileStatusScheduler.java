package com.salescode.dis.insights.scheduler;

import com.salescode.dis.insights.dto.FileStatusRequestDto;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final FileService fileService;

    @Scheduled(fixedRateString = "${file-status-scheduler.rate-millis:60000}") // Run every 1 minute (60000 ms)
    @Transactional
    public void updateAllFileStatus() {
        log.info("Starting scheduled update of all file statuses");

        // Define the time window for staleness
        Instant staleCutoffTime = Instant.now().minus(STALE_THRESHOLD_SECONDS, ChronoUnit.SECONDS); // e.g., 10 mins ago
        Instant tooOldCutoffTime = Instant.now().minus(TOO_OLD_THRESHOLD_SECONDS, ChronoUnit.SECONDS); // e.g., 15 mins ago

        // Find all files that are PENDING and haven't been modified in the 10-15 min window
        // todo remove the check on status as PENDING
        List<FileEntity> pendingFiles = fileRepository.findAllStalePendingFiles(staleCutoffTime, tooOldCutoffTime, FileStatus.PENDING);

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
        FileStatus consumedStatus = null;
        FileStatus publishedStatus = null;
        boolean updateNeeded = false;

        // Check if consumed status is PENDING and update if needed
        if (file.getConsumedStatus() == FileStatus.PENDING) {
            long totalConsumed = file.getConsumedSuccessCount() + file.getConsumedFailCount();
            long totalPublished = file.getPublishedSuccessCount() + file.getPublishedFailCount();
            if (totalConsumed > 0) {
                if (totalConsumed == totalPublished) {
                    if(file.getConsumedFailCount() == 0 && file.getPublishedFailCount()==0) {
                        consumedStatus = FileStatus.COMPLETED_SUCCESSFULLY;
                    }
                    else if(file.getConsumedFailCount() > 0){
                        consumedStatus = FileStatus.COMPLETED_WITH_FAILURES;
                    }
                    log.info("Will update file {} consumed status to COMPLETED", file.getId());
                } else {
                    consumedStatus = FileStatus.FAILED;
                    log.info("Will update file {} consumed status to FAILED", file.getId());
                }
                updateNeeded = true;
            }
        }

        // Check if published status is PENDING and update if needed
        if (file.getPublishedStatus() == FileStatus.PENDING) {
            long totalConsumed = file.getConsumedSuccessCount() + file.getConsumedFailCount();
            long totalPublished = file.getPublishedSuccessCount() + file.getPublishedFailCount();
            if (totalPublished > 0) {
                if (totalPublished == totalConsumed) {
                    if(file.getPublishedFailCount()==0) {
                        publishedStatus = FileStatus.COMPLETED_SUCCESSFULLY;
                    }
                    else if(file.getPublishedFailCount() > 0){
                        publishedStatus = FileStatus.COMPLETED_WITH_FAILURES;
                    }
                    log.info("Will update file {} published status to COMPLETED", file.getId());
                } else {
                    publishedStatus = FileStatus.FAILED;
                    log.info("Will update file {} published status to FAILED", file.getId());
                }
                updateNeeded = true;
            }
        }

        // Update the file status using FileService if any status needs to be updated
        if (updateNeeded) {
            FileStatusRequestDto statusRequest = new FileStatusRequestDto();
            statusRequest.setConsumedStatus(consumedStatus);
            statusRequest.setPublishedStatus(publishedStatus);

            fileService.updateStatus(file.getFileId(), file.getMaster(),statusRequest);
            log.info("Updated file {} status using FileService", file.getId());
        }
    }
}
