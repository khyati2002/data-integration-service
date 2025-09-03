package com.salescode.dis.insights.repository;


import com.salescode.dis.insights.dto.MetadataEntry;
import com.salescode.dis.insights.entity.InsightsMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InsightsMetadataRepository extends JpaRepository<InsightsMetadata, String> {
    Optional<InsightsMetadata> findByKey(String key);

    @Query("SELECT i.key AS key, i.value AS value FROM InsightsMetadata i WHERE i.key LIKE CONCAT(:lob, '%')")
    List<MetadataEntry> findKeyValueByLobPrefix(@Param("lob") String lob);

}