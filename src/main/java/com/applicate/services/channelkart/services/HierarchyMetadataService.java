package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.repository.HierarchyMetadataRepository;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;

public class HierarchyMetadataService extends AbstractCDMService<HierarchyMetadata>{

    private static HierarchyMetadataRepository hierarchyMetadataRepository;

    public HierarchyMetadataService( DSLContext dsl){
        super(dsl);
        hierarchyMetadataRepository = new HierarchyMetadataRepository(dsl);
    }
    @Override
    public List<HierarchyMetadata> batchSave(List<HierarchyMetadata> cdmObject) {
        return List.of();
    }

    public Collection<HierarchyMetadata> findByImmediateParent(String loginId){
        return hierarchyMetadataRepository.findByImmediateParent(loginId);
    }

    public HierarchyMetadata findByHierarchy(String hierarchy) {
        return hierarchyMetadataRepository.findByHierarchy(hierarchy);
    }
}
