package com.salescode.dis.insights.repository;

import com.salescode.dis.insights.entity.StageMetadata;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageMetadataRepository extends JpaRepository<StageMetadata, Long> {
    List<StageMetadata> findByMode(ModeOfIntegration mode);
    List<StageMetadata> findByModeIn(List<ModeOfIntegration> modes);
} 