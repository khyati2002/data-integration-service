package com.salescode.dis.insights.repository;


import com.salescode.dis.insights.entity.InsightsMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InsightsMetadataRepository extends JpaRepository<InsightsMetadata, String> {
    Optional<InsightsMetadata> findByKey(String key);
}