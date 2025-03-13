package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.repository.HierarchyMetadataRepository;
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

    @Cacheable
    public List<HierarchyMetadata> findByImmediateParent(String loginId) {
        return hierarchyMetadataRepository.findByImmediateParent(loginId);
    }

    public HierarchyMetadata findByHierarchy(String hierarchy) {
        return hierarchyMetadataRepository.findByHierarchy(hierarchy);
    }

    public List<HierarchyMetadata> batchSave(List<HierarchyMetadata> hierarchyMetadataList){
        List<String> hierarchyList = hierarchyMetadataList.stream()
                .map(HierarchyMetadata::getHierarchy)
                .collect(Collectors.toList());

        Map<String, com.salescode.dim.jooq.generated.tables.pojos.HierarchyMetadata> savedList = getDslContext().selectFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.HIERARCHY.in(hierarchyList))
                .fetch()
                .intoMap(CK_HIERARCHY_METADATA.HIERARCHY, record -> record.into(com.salescode.dim.jooq.generated.tables.pojos.HierarchyMetadata.class));

        List<com.salescode.dim.jooq.generated.tables.pojos.HierarchyMetadata> itemsToInsert = new ArrayList<>();
        List<com.salescode.dim.jooq.generated.tables.pojos.HierarchyMetadata> itemsToUpdate = new ArrayList<>();
        for (int i = 0; i < hierarchyMetadataList.size(); i++) {
           // super.addHash(hierarchyMetadataList.get(i));
            if (savedList.get(hierarchyMetadataList.get(i).getHierarchy()) == null) {
                hierarchyMetadataList.get(i).setVersion(0);
                hierarchyMetadataList.get(i).setId(UUID.randomUUID().toString());
                itemsToInsert.add(hierarchyMetadataList.get(i));
            } else {
                if (!Objects.equals(hierarchyMetadataList.get(i).getHash(), savedList.get(hierarchyMetadataList.get(i).getHierarchy()).getHash())) {
                    hierarchyMetadataList.get(i).setId(savedList.get(hierarchyMetadataList.get(i).getHierarchy()).getId());
                    hierarchyMetadataList.get(i).setVersion(savedList.get(hierarchyMetadataList.get(i).getHierarchy()).getVersion());
                    itemsToUpdate.add(hierarchyMetadataList.get(i));
                }
            }
        }
        if (!itemsToInsert.isEmpty()) {
            getDslContext().batchInsert(
                    itemsToInsert.stream()
                            .map(hierarchy -> getDslContext().newRecord(CK_HIERARCHY_METADATA, hierarchy)) // Convert to jOOQ Records
                            .collect(Collectors.toList())
            ).execute();
        }

        if (!itemsToUpdate.isEmpty()) {
            getDslContext().batchUpdate(
                    itemsToUpdate.stream()
                            .map(hierarchy -> {
                                CkHierarchyMetadataRecord record = getDslContext().newRecord(CK_HIERARCHY_METADATA, hierarchy);
                                record.changed(CK_HIERARCHY_METADATA.ID, false); // Avoid updating primary key
                                return record;
                            })
                            .collect(Collectors.toList())
            ).execute();

        }
        return hierarchyMetadataList;
    }


}
