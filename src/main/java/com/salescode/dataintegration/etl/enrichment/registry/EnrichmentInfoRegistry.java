package com.salescode.dataintegration.etl.enrichment.registry;

import com.salescode.channelkart.converters.ActiveStatus;
import com.salescode.channelkart.converters.EnrichmentPhase;
import com.salescode.dataintegration.etl.interfaces.RefreshableRegistry;
import com.salescode.jooq.generated.tables.pojos.CkEnrichmentInfo;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.salescode.jooq.generated.Tables.CK_ENRICHMENT_INFO;

@Service
public class EnrichmentInfoRegistry implements RefreshableRegistry {

    private final DSLContext dsl;
    private final Map<EnrichmentPhase, List<CkEnrichmentInfo>> enrichmentCache = new ConcurrentHashMap<>();

    @Autowired
    public EnrichmentInfoRegistry(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Retrieve a list of active CkEnrichmentInfo by phase, loading from the database if not cached.
     *
     * @param phase the enrichment phase
     * @return list of CkEnrichmentInfo if found and active
     * @throws IllegalArgumentException if no enrichment info is found for the phase
     */
    public List<CkEnrichmentInfo> getEnrichmentInfoByPhase(EnrichmentPhase phase) {
        // Check cache by phase, and load from DB if absent
        return enrichmentCache.computeIfAbsent(phase, this::loadEnrichmentsByPhase);
    }

    /**
     * Load a list of enrichments by phase from the database if active.
     *
     * @param phase the enrichment phase
     * @return list of CkEnrichmentInfo or an empty list if none are found or active
     */
    private List<CkEnrichmentInfo> loadEnrichmentsByPhase(EnrichmentPhase phase) {
        return dsl.selectFrom(CK_ENRICHMENT_INFO)
                .where(CK_ENRICHMENT_INFO.PHASE.eq(phase))
                .and(CK_ENRICHMENT_INFO.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
                .fetchInto(CkEnrichmentInfo.class);
    }

    /**
     * Clear the cache, forcing fresh database loads for future requests.
     */
    @Override
    public void refreshRegistry() {
        enrichmentCache.clear();
    }
}