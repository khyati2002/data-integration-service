package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.cache.DistributedCache;
import com.applicate.services.channelkart.repository.HierarchyMetadataRepository;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.cache.CacheKeys;
import com.salescode.dim.jooq.impl.HierarchyMetadata;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_HIERARCHY_METADATA;

public class HierarchyMetadataService extends AbstractCDMService<HierarchyMetadata> {
    private static HierarchyMetadataRepository hierarchyMetadataRepository;
    private final DistributedCache distributedCache;

    public HierarchyMetadataService() {
        hierarchyMetadataRepository = new HierarchyMetadataRepository(getDslContext());
        distributedCache = DistributedCache.getInstance();
    }

    public List<HierarchyMetadata> findByImmediateParent(String loginId) {
        return findByImmediateParent(loginId, true);
    }

    public List<HierarchyMetadata> findByImmediateParent(String loginId, boolean cache) {
        if (loginId == null) {
            return Collections.emptyList();
        }
        Function<String, List<HierarchyMetadata>> loader = (String id) -> hierarchyMetadataRepository.findByImmediateParent(id);
        return cache ? distributedCache.withCache(SecurityContextUtils.getLob(), CacheKeys.HIERARCHY_METADATA_CACHE_DOMAIN, loginId, loader) : loader.apply(loginId);
    }


    public HierarchyMetadata findByHierarchy(String hierarchy) {
        return hierarchyMetadataRepository.findByHierarchy(hierarchy);
    }

    public List<HierarchyMetadata> findByHierarchyIn(Set<String> hierarchyStrings) {
        if (hierarchyStrings == null || hierarchyStrings.isEmpty()) {
            return Collections.emptyList();
        }

        return hierarchyMetadataRepository.findByHierarchyIn(hierarchyStrings);
    }

    public List<HierarchyMetadata> batchSave(List<HierarchyMetadata> hierarchyMetadataList){
        hierarchyMetadataList.forEach(this::fillCommonAttributes);
        if (!hierarchyMetadataList.isEmpty()) {
            getDslContext().batchInsert(
                    hierarchyMetadataList.stream()
                            .map(hierarchy -> getDslContext().newRecord(CK_HIERARCHY_METADATA, hierarchy)) // Convert to jOOQ Records
                            .collect(Collectors.toList())
            ).execute();
        }

        DistributedCache.getInstance().evictAll(CacheKeys.HIERARCHY_METADATA_CACHE_DOMAIN);
        return hierarchyMetadataList;
    }


}
