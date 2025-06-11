package com.salescode.dis.insights.repository;

import com.salescode.dis.insights.entity.IntegrationStageProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IntegrationStageProgressRepository extends JpaRepository<IntegrationStageProgress, Long> {
    List<IntegrationStageProgress> findByFileId(Long fileId);
    Optional<IntegrationStageProgress> findByFileIdAndStage(Long fileId, String stage);
    List<IntegrationStageProgress> findByFileIdAndIsCompleted(Long fileId, Boolean isCompleted);
} 