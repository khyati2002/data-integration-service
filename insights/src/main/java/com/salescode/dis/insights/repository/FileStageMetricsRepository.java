package com.salescode.dis.insights.repository;

import com.salescode.dis.insights.dto.job.JobStageAccumulatedData;
import com.salescode.dis.insights.dto.LobSummaryDto;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.enums.ProgressStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            "j.id, j.creationTime, j.lastModifiedTime, j.lob," +
            "CAST(j.extendedAttributes AS string), " +
            "j.startTime, j.endTime, j.status ," +
            "j.publisherJobUri, j.consumerJobUri, " +
            "s.stageType, CAST(COALESCE(SUM(s.successCount), 0) AS long)) " +
            "FROM JobEntity j JOIN FileStageMetrics s ON j.id = s.job.id " +
            "WHERE j.lob = :lob AND j.lastModifiedTime BETWEEN :startDate AND :endDate " +
            "GROUP BY j.id, j.creationTime, j.lastModifiedTime, j.lob, j.startTime, j.endTime, j.status, " +
            "j.publisherJobUri, j.consumerJobUri, s.stageType")
    List<JobStageAccumulatedData> findJobsWithAggregatedStagesByLob(@Param("lob") String lob,    @Param("startDate") Instant startDate,
                                                                    @Param("endDate") Instant endDate);



}