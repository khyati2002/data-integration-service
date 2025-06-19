package com.salescode.dis.insights.repository;

import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.enums.ProgressStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileStageMetricsRepository extends JpaRepository<FileStageMetrics, String> {
    boolean existsFileStageMetricsByFile_IdAndStageType(String fileId, ProgressStage stageType);

    Optional<FileStageMetrics> findByFileAndStageType(FileEntity file, ProgressStage stageType);
}