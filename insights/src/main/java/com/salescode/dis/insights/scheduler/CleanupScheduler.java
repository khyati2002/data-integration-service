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
            // Delete FileStageMetrics older than cutoff
            int metricsDeleted = fileStageMetricsRepository.deleteByLastModifiedBefore(cutoffTime);
            log.info("Deleted {} FileStageMetrics older than cutoff", metricsDeleted);

            // Delete FileEntity older than cutoff
            int filesDeleted = fileRepository.deleteByLastModifiedBefore(cutoffTime);
            log.info("Deleted {} FileEntities older than cutoff", filesDeleted);

            // Delete Job older than cutoff
            int jobsDeleted = jobRepository.deleteByLastModifiedBefore(cutoffTime);
            log.info("Deleted {} Jobs older than cutoff", jobsDeleted);

            log.info("Cleanup of old entities completed successfully.");
        } catch (Exception e) {
            log.error("Exception occurred during cleanupOldEntities execution: {}", e.getMessage(), e);
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
