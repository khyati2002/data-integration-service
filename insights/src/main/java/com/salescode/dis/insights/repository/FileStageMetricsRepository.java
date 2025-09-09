package com.salescode.dis.insights.repository;

import com.salescode.dis.insights.dto.job.JobStageAccumulatedData;
import com.salescode.dis.insights.dto.LobSummaryDto;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ProgressStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import java.util.Optional;

public interface FileStageMetricsRepository extends JpaRepository<FileStageMetrics, String> {
    boolean existsFileStageMetricsByFile_IdAndStageType(String fileId, ProgressStage stageType);

    Optional<FileStageMetrics> findByFileAndStageType(FileEntity file, ProgressStage stageType);

    List<FileStageMetrics> findByJobIdIn(List<String> jobIds);

    @Query("SELECT new com.salescode.dis.insights.dto.LobSummaryDto(" +
            "f.job.lob, " + // Keep this to get the LOB in the DTO
            "AVG(f.throughput), " +
            "MAX(f.throughput), " +
            "SUM(CASE WHEN f.stageType = :queueStage THEN f.successCount ELSE 0 END), " +
            "SUM(CASE WHEN f.stageType = :queueStage THEN (f.logicalFailureCount + f.serverFailureCount) ELSE 0 END), " +
            "SUM(CASE WHEN f.stageType = :saveStage THEN f.successCount ELSE 0 END), " +
            "SUM(CASE WHEN f.stageType = :saveStage THEN (f.logicalFailureCount + f.serverFailureCount) ELSE 0 END), " +
            "COUNT(DISTINCT CASE WHEN j.status = 'PENDING' THEN j.id ELSE NULL END), " +
            "COUNT(DISTINCT CASE WHEN j.status IN ('COMPLETED_SUCCESSFULLY', 'COMPLETED_UNSUCCESSFULLY') THEN j.id ELSE NULL END), " +
            "COUNT(DISTINCT CASE WHEN j.status = 'FAILED' THEN j.id ELSE NULL END)) " +
            "FROM FileStageMetrics f JOIN f.job j " +
            "WHERE f.job.lob IN :lobs " + // Changed to IN clause
            "AND f.lastModifiedTime BETWEEN :startDate AND :endDate " +
            "GROUP BY f.job.lob")
    List<LobSummaryDto> findAggregateMetricsAndJobCountsByLobAndDateRange(
                                                                           @Param("lobs") List<String> lobs, // Change parameter type to List<String>
                                                                           @Param("queueStage") ProgressStage queueStage,
                                                                           @Param("saveStage") ProgressStage saveStage,
                                                                           @Param("startDate") Instant startDate,
                                                                           @Param("endDate") Instant endDate);

    @Query("SELECT new com.salescode.dis.insights.dto.job.JobStageAccumulatedData(" +
            "j.id, s.master, s.modeOfIntegration, j.creationTime, j.lastModifiedTime, j.lob," +
            "CAST(j.extendedAttributes AS string), " +
            "j.startTime, j.endTime, j.status ," +
            "j.publisherJobUri, j.consumerJobUri, " +
            "s.stageType, CAST(COALESCE(SUM(s.successCount), 0) AS long),CAST(COALESCE(SUM(s.serverFailureCount), 0) AS long), CAST(COALESCE(SUM(s.logicalFailureCount), 0) AS long))" +
            "FROM JobEntity j JOIN FileStageMetrics s ON j.id = s.job.id " +
            "WHERE j.lob = :lob AND j.lastModifiedTime BETWEEN :startDate AND :endDate " +
            "GROUP BY j.id, j.creationTime, j.lastModifiedTime, j.lob, j.startTime, j.endTime, j.status, " +
            "j.publisherJobUri, j.consumerJobUri, s.stageType, s.master, s.modeOfIntegration")
    List<JobStageAccumulatedData> findJobsWithAggregatedStagesByLob(@Param("lob") String lob,    @Param("startDate") Instant startDate,
                                                                    @Param("endDate") Instant endDate);

    @Query("SELECT new com.salescode.dis.insights.dto.job.JobStageAccumulatedData(" +
            "j.id, s.master, s.modeOfIntegration, j.creationTime, j.lastModifiedTime, j.lob," +
            "CAST(j.extendedAttributes AS string), " +
            "j.startTime, j.endTime, j.status ," +
            "j.publisherJobUri, j.consumerJobUri, " +
            "s.stageType, CAST(COALESCE(SUM(s.successCount), 0) AS long),CAST(COALESCE(SUM(s.serverFailureCount), 0) AS long), CAST(COALESCE(SUM(s.logicalFailureCount), 0) AS long))" +
            "FROM JobEntity j JOIN FileStageMetrics s ON j.id = s.job.id " +
            "WHERE j.lob = :lob AND j.lastModifiedTime BETWEEN :startDate AND :endDate AND s.modeOfIntegration = :modeOfIntegration " +
            "GROUP BY j.id, j.creationTime, j.lastModifiedTime, j.lob, j.startTime, j.endTime, j.status, " +
            "j.publisherJobUri, j.consumerJobUri, s.stageType, s.master, s.modeOfIntegration")
    List<JobStageAccumulatedData> findJobsWithAggregatedStagesByLobAndMode(@Param("lob") String lob, @Param("startDate") Instant startDate,
                                                                           @Param("endDate") Instant endDate, @Param("modeOfIntegration")ModeOfIntegration modeOfIntegration);


    @Query("SELECT s FROM FileStageMetrics s " +
            "WHERE (s.modeOfIntegration = com.salescode.dis.insights.enums.ModeOfIntegration.CK_API_CLIENT " +
            "       OR s.modeOfIntegration = com.salescode.dis.insights.enums.ModeOfIntegration.CK_STREAMLET_SYNC " +
            "       OR s.modeOfIntegration = com.salescode.dis.insights.enums.ModeOfIntegration.CK_MDM_KAFKA " +
            "       OR s.modeOfIntegration = com.salescode.dis.insights.enums.ModeOfIntegration.CK_ORDER_PUSH) " +
            "  AND s.progressStatus = :status " +
            "  AND s.lastModifiedTime < :staleCutoffTime " +
            "  AND s.lastModifiedTime >= :tooOldCutoffTime")
    List<FileStageMetrics> findStalePendingStages(
            @Param("staleCutoffTime") Instant staleCutoffTime,
            @Param("tooOldCutoffTime") Instant tooOldCutoffTime,
            @Param("status") ProgressStatus status
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM FileStageMetrics fsm WHERE fsm.lastModifiedTime < :cutoffTime")
    int deleteByLastModifiedBefore(Instant cutoffTime);

}