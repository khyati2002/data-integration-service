package com.salescode.dis.insights.service;

import com.salescode.dis.insights.entity.InsightsMetadata;
import com.salescode.dis.insights.repository.InsightsMetadataRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class InsightsMetadataService {

    private final InsightsMetadataRepository metadataRepository;

    public InsightsMetadataService(InsightsMetadataRepository metadataRepository) {
        this.metadataRepository = metadataRepository;
    }

    @Cacheable(value = "queries", key = "#key")
    public String getQuery(String key) {
        Optional<InsightsMetadata> opt = metadataRepository.findByKey(key);
        return opt.map(InsightsMetadata::getValue).orElse(null);
    }

    @CacheEvict(value = "queries", key = "#key")
    public void evictQuery(String key) { /* just evicts */ }

    @CacheEvict(value = "queries", allEntries = true)
    public void evictAllQueries() { /* no-op */ }
}
