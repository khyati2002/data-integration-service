package com.salescode.dim.etl.enrichment.service;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.interfaces.RefreshableRegistry;
import com.salescode.dim.jooq.generated.tables.pojos.EnrichmentInfo;
import lombok.AllArgsConstructor;
import org.jooq.DSLContext;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_ENRICHMENT_INFO;

public class EnrichmentInfoRegistry implements RefreshableRegistry, Serializable {

    private static final long serialVersionUID = -4561811180546832037L;
    private final Map<CacheKey, List<EnrichmentInfo>> enrichmentCache = new ConcurrentHashMap<>();
    private final transient DSLContext dsl;

    /**
     * Creates the registry and preloads all enrichment data
     *
     * @param dsl the DSLContext for database operations
     */
    public EnrichmentInfoRegistry(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "DSLContext cannot be null");
        init();
    }

    /**
     * Initializes the registry by loading all enrichment data for all phases
     */
    public void init() {
        List<String> phases = fetchAllPhases();
        phases.forEach(this::loadEnrichmentsByPhase);
    }

    /**
     * Fetches all distinct phases from the database
     *
     * @return list of all distinct phases
     */
    private List<String> fetchAllPhases() {
        return dsl.selectDistinct(CK_ENRICHMENT_INFO.PHASE).from(CK_ENRICHMENT_INFO).fetch(CK_ENRICHMENT_INFO.PHASE);
    }

    /**
     * Retrieve a list of active EnrichmentInfo by phase from the cache
     *
     * @param phase the enrichment phase
     * @return list of EnrichmentInfo for the specified phase
     */
    public List<EnrichmentInfo> getEnrichmentInfoByPhase(String phase) {
        if (phase == null) {
            return Collections.emptyList();
        }

        return enrichmentCache.entrySet()
                              .stream()
                              .filter(entry -> phase.equals(entry.getKey().phase))
                              .map(Map.Entry::getValue)
                              .flatMap(List::stream)
                              .collect(Collectors.toList());
    }

    /**
     * Retrieve a list of active EnrichmentInfo by phase and type from the cache
     *
     * @param phase the enrichment phase
     * @param type  the data model type
     * @return list of EnrichmentInfo for the specified phase and type
     */
    public List<EnrichmentInfo> getEnrichmentInfoByPhaseAndType(String phase, String type) {
        if (phase == null || type == null) {
            return Collections.emptyList();
        }

        CacheKey key = new CacheKey(phase, type);
        return enrichmentCache.getOrDefault(key, Collections.emptyList());
    }

    /**
     * Load a list of enrichments by phase from the database if active.
     *
     * @param phase the enrichment phase
     * @return list of EnrichmentInfo or an empty list if none are found or active
     */
    private List<EnrichmentInfo> loadEnrichmentsByPhase(String phase) {
        List<EnrichmentInfo> enrichmentInfos = dsl.selectFrom(CK_ENRICHMENT_INFO)
                                                  .where(CK_ENRICHMENT_INFO.PHASE.eq(phase))
                                                  .and(CK_ENRICHMENT_INFO.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
                                                  .fetchInto(EnrichmentInfo.class);

        // Group the enrichments by type and store them with phase+type as key
        Map<String, List<EnrichmentInfo>> groupedByType = enrichmentInfos.stream()
                                                                         .collect(Collectors.groupingBy(EnrichmentInfo::getType));

        // Store each type group in the cache with a composite key
        groupedByType.forEach((type, infoList) -> enrichmentCache.put(new CacheKey(phase, type), infoList));

        return enrichmentInfos;
    }

    /**
     * Clear the cache, forcing fresh database loads for future requests.
     */
    @Override
    public void refreshRegistry() {
        enrichmentCache.clear();
        init();
    }

    /**
     * Composite key class for the enrichment cache
     */
    @AllArgsConstructor
    private static class CacheKey implements Serializable {

        private static final long serialVersionUID = 1L;

        final String phase;
        final String type;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return (Objects.equals(phase, cacheKey.phase) && Objects.equals(type, cacheKey.type));
        }

        @Override
        public int hashCode() {
            return Objects.hash(phase, type);
        }
    }
}
