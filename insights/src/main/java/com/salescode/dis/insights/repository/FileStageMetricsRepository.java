package com.salescode.dis.insights.repository;

import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.enums.ProgressStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.List;
import java.util.Optional;

public interface FileStageMetricsRepository extends JpaRepository<FileStageMetrics, String> {
    boolean existsFileStageMetricsByFile_IdAndStageType(String fileId, ProgressStage stageType);

    Optional<FileStageMetrics> findByFileAndStageType(FileEntity file, ProgressStage stageType);

    List<FileStageMetrics> findByJobIdIn(List<String> jobIds);
    @Query("SELECT " +
            "AVG(f.throughput), " +
            "MAX(f.throughput), " +
            "SUM(CASE WHEN f.stageType = :queueStage THEN f.successCount ELSE 0 END), " +
            "SUM(CASE WHEN f.stageType = :queueStage THEN (f.logicalFailureCount + f.serverFailureCount)  ELSE 0 END), " +
            "SUM(CASE WHEN f.stageType = :saveStage THEN f.successCount ELSE 0 END), " +
            "SUM(CASE WHEN f.stageType = :saveStage THEN (f.logicalFailureCount + f.serverFailureCount)  ELSE 0 END) " +
            "FROM FileStageMetrics f " +
            "WHERE f.job.lob = :lob")
    List<Object[]> findAggregateMetricsByLob(@Param("lob") String lob,
                                             @Param("queueStage") ProgressStage queueStage,
                                             @Param("saveStage") ProgressStage saveStage);


}