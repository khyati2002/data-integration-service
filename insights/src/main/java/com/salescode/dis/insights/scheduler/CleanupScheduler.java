package com.salescode.dis.insights.scheduler;

import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.repository.FileStageMetricsRepository;
import com.salescode.dis.insights.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class CleanupScheduler {

    @Value("${file-cleanup.scheduler.cleanup-threshold-days:15}")
    private int cleanupThresholdDays;

    private final FileStageMetricsRepository fileStageMetricsRepository;
    private final FileRepository fileRepository;
    private final JobRepository jobRepository;
    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "${file-cleanup.scheduler.cron:0 0 16 * * ?}", zone = "Asia/Kolkata")
    @Transactional
    public void cleanupOldEntities() {
        Instant cutoffTime = Instant.now().minus(cleanupThresholdDays, ChronoUnit.DAYS);
        log.info("Starting cleanup of entities older than {} days (before {})", cleanupThresholdDays, cutoffTime);

        try {
            cleanupByOldJobs(cutoffTime);
            cleanUpFiles(cutoffTime);

            log.info("Cleanup of old entities completed successfully.");
        } catch (Exception e) {
            log.error("Exception occurred during cleanupOldEntities execution: {}", e.getMessage(), e);
        }
    }

    private void cleanupByOldJobs(Instant cutoffTime) {
        // Find job IDs that are 15+ days old
        List<String> oldJobIds = jobRepository.findJobIdsOlderThan(cutoffTime);

        if (!oldJobIds.isEmpty()) {
            log.info("Found {} old job IDs to cleanup", oldJobIds.size());

            // Delete file_stage_metrics by job_id
            int metricsDeleted = fileStageMetricsRepository.deleteByJobIdIn(oldJobIds);
            log.info("Deleted {} FileStageMetrics by job IDs", metricsDeleted);

            // Delete files by job_id
            int filesDeleted = fileRepository.deleteByJobIdIn(oldJobIds);
            log.info("Deleted {} FileEntities by job IDs", filesDeleted);

            // Delete the jobs themselves
            int jobsDeleted = jobRepository.deleteByIdIn(oldJobIds);
            log.info("Deleted {} Jobs", jobsDeleted);
        } else {
            log.info("No old jobs found for cleanup");
        }
    }

    private void cleanUpFiles(Instant cutoffTime) {
        // Find file IDs that are 15+ days old and not already deleted
        List<String> oldFileIds = fileRepository.findFileIdsOlderThan(cutoffTime);

        if (!oldFileIds.isEmpty()) {
            log.info("Found {} old file IDs to cleanup", oldFileIds.size());

            // Delete file_stage_metrics by file_id
            int metricsDeleted = fileStageMetricsRepository.deleteByFileIdIn(oldFileIds);
            log.info("Deleted {} FileStageMetrics by file IDs", metricsDeleted);

            // Delete the files themselves
            int filesDeleted = fileRepository.deleteByIdIn(oldFileIds);
            log.info("Deleted {} remaining FileEntities", filesDeleted);
        } else {
            log.info("No orphaned files found for cleanup");
        }
    }

    @Scheduled(cron = "0 */30 * * * *", zone = "Asia/Kolkata")
    public void cleanupExpiredPartitions() {
        try {
            String result = jdbcTemplate.queryForObject(
                    "SELECT cleanup_expired_partitions()", String.class);
            log.info("[Partition Cleanup] {}", result);
        } catch (Exception e) {
            log.error("Exception occurred during cleanupExpiredPartitions: {}", e.getMessage(), e);
        }
    }
}
