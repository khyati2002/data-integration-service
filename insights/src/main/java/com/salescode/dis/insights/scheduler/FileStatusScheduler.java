package com.salescode.dis.insights.scheduler;

import com.salescode.dis.insights.dto.FileStatusRequestDto;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileStatusScheduler {

    private final FileRepository fileRepository;
    private final FileService fileService;

    @Scheduled(fixedRate = 60000) // Run every 1 minute (60000 ms)
    @Transactional
    public void updateApiBasedFileStatus() {
        log.info("Starting scheduled update of API-based file statuses");

        // Find API-based files modified in the last 10 minutes with PENDING status
        Instant cutoffTime = Instant.now().minus(10, ChronoUnit.MINUTES);
        List<FileEntity> pendingFiles = fileRepository.findApiBasedFilesModifiedSince(cutoffTime, FileStatus.PENDING);

        log.info("Found {} API-based files with PENDING status modified in the last 10 minutes", pendingFiles.size());

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
            int totalConsumed = file.getConsumedSuccessCount() + file.getConsumedFailCount();
            int totalPublished = file.getPublishedSuccessCount() + file.getPublishedFailCount();
            if (totalConsumed > 0) {
                if (totalConsumed == totalPublished) {
                    consumedStatus = FileStatus.COMPLETED;
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
            int totalConsumed = file.getConsumedSuccessCount() + file.getConsumedFailCount();
            int totalPublished = file.getPublishedSuccessCount() + file.getPublishedFailCount();
            if (totalPublished > 0) {
                if (totalPublished == totalConsumed) {
                    publishedStatus = FileStatus.COMPLETED;
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

            fileService.updateStatus(file.getId(), statusRequest);
            log.info("Updated file {} status using FileService", file.getId());
        }
    }
}
