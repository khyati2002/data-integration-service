package com.salescode.dataintegration.etl.enrichment.registry;

import com.salescode.channelkart.enrichments.EnrichmentInfo;
import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.models.enums.EnrichmentPhase;
import com.salescode.channelkart.repository.EnrichmentInfoRepository;
import com.salescode.dataintegration.etl.interfaces.RefreshableRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class EnrichmentInfoRegistry extends RefreshableRegistry {

    private final Map<EnrichmentPhase, List<EnrichmentInfo>> enrichmentCache = new ConcurrentHashMap<>();
    private final EnrichmentInfoRepository enrichmentInfoRepository;

    @Autowired
    public EnrichmentInfoRegistry(EnrichmentInfoRepository enrichmentInfoRepository) {
        this.enrichmentInfoRepository = enrichmentInfoRepository;
    }

    /**
     * Retrieve a list of active EnrichmentInfo by phase, loading from the database if not cached.
     *
     * @param phase the enrichment phase
     * @return list of EnrichmentInfo if found and active
     * @throws IllegalArgumentException if no enrichment info is found for the phase
     */
    public List<EnrichmentInfo> getEnrichmentInfoByPhase(EnrichmentPhase phase) {
        return enrichmentCache.computeIfAbsent(phase, this::loadEnrichmentsByPhase);
    }

    /**
     * Load a list of enrichments by phase from the database if active.
     *
     * @param phase the enrichment phase
     * @return list of EnrichmentInfo or an empty list if none are found or active
     */
    private List<EnrichmentInfo> loadEnrichmentsByPhase(EnrichmentPhase phase) {
        return enrichmentInfoRepository.findByPhaseAndActiveStatus(phase, ActiveStatus.ACTIVE);
    }

    /**
     * Clear the cache, forcing fresh database loads for future requests.
     */
    @Override
    public void refreshRegistry() {
        enrichmentCache.clear();
    }
}