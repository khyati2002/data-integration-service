package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.repository.HierarchyMetadataRepository;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.jooq.generated.tables.records.CkHierarchyMetadataRecord;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import org.jooq.DSLContext;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_HIERARCHY_METADATA;

public class HierarchyMetadataService extends AbstractCDMService<HierarchyMetadata> {
    private static HierarchyMetadataRepository hierarchyMetadataRepository;

    public HierarchyMetadataService() {
        hierarchyMetadataRepository = new HierarchyMetadataRepository(getDslContext());
    }

    @Cacheable(cacheName = "dataintegration-hierarchymetadatas")
    public List<HierarchyMetadata> findByImmediateParent(String loginId) {
        return hierarchyMetadataRepository.findByImmediateParent(loginId);
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

        CacheManager.getInstance().evictAll("dataintegration-hierarchymetadatas");
        return hierarchyMetadataList;
    }


}
