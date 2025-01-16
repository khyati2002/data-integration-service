package com.applicate.services.channelkart.enrichments;

import com.applicate.services.channelkart.cache.AllLOBRouter;
import com.applicate.services.channelkart.cache.AppCacheManager;
import com.applicate.services.channelkart.cache.DistributedCache;
import com.applicate.services.channelkart.registry.AbstractRegistry;
import com.applicate.services.channelkart.services.SpringContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EnrichmentRegistry extends AbstractRegistry<EnrichmentInfo> {

    public static final EnrichmentRegistry INSTANCE = new EnrichmentRegistry();

    private static final List<EnrichmentInfo> EMPTY_ENRICHMENTS = Collections.unmodifiableList(new ArrayList<>());
    private static final String CACHE_DOMAIN = "enrichment";
    private final DistributedCache distributedCache = SpringContext.getBean(DistributedCache.class);

    private EnrichmentRegistry() {
    }

    @Override
    public void add(EnrichmentInfo enrichmentInfo) {
        synchronized (enrichmentInfo.getLob().intern()) {
            List<EnrichmentInfo> rules = (List<EnrichmentInfo>) distributedCache.get(enrichmentInfo.getLob(), null, CACHE_DOMAIN, false);
            if (rules == null) {
                rules = new ArrayList<EnrichmentInfo>();
            }
            rules.add(enrichmentInfo);
            clear(enrichmentInfo.getLob());
            distributedCache.put(enrichmentInfo.getLob(), null, CACHE_DOMAIN, rules, false);
        }
    }

    @Override
    public void addAll(String lob, List<EnrichmentInfo> enrichmentInfo) {
        synchronized (lob.intern()) {
            clear(lob);
            distributedCache.put(lob, null, CACHE_DOMAIN, enrichmentInfo, false);
        }
    }

    @Override
    public List<EnrichmentInfo> get(String lob) {

        List<EnrichmentInfo> enrichments = AppCacheManager.getInstance().withCache(lob, CACHE_DOMAIN, (sk) -> distributedCache.withCache(lob, null, CACHE_DOMAIN, (s) -> AllLOBRouter.loadAll(EnrichmentInfo.class, (k) -> k.equalsIgnoreCase(lob)).stream().map(e -> (EnrichmentInfo) e).collect(Collectors.toList())));
        return lob != null && enrichments != null ? enrichments : EMPTY_ENRICHMENTS;
    }

    @Override
    public List<EnrichmentInfo> get(String lob, String type) {
        return get(lob, (rule) -> rule.getType().equalsIgnoreCase(type) && rule.isEnabled());

    }

    public List<EnrichmentInfo> get(String lob, String type, EnrichmentPhase phase) {
        return get(lob, rule -> (rule.getType().equalsIgnoreCase(type) && rule.getPhase().equals(phase) && rule.isEnabled()));
    }

    @Override
    public void loadAll(Predicate<String> predicate, boolean overrideOld) {
        AllLOBRouter.loadAll(EnrichmentInfo.class, predicate).stream().map(e -> (EnrichmentInfo) e).collect(Collectors.groupingBy(a -> a.getLob())).entrySet().stream().forEach(e -> {
            if (distributedCache.get(e.getKey(), null, CACHE_DOMAIN, false) == null || overrideOld)
                addAll(e.getKey(), e.getValue());
        });
    }

    @Override
    public void clear(String lob) {
        distributedCache.clearCache(lob, null, CACHE_DOMAIN);
    }

}
