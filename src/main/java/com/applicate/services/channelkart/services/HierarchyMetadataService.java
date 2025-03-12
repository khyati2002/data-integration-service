package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.impl.HierarchyMetadata;

import java.util.List;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_HIERARCHY_METADATA;

public class HierarchyMetadataService extends AbstractCDMService<HierarchyMetadata> {

    public List<HierarchyMetadata> findByImmediateParent(String loginId) {
        List<com.salescode.dim.jooq.generated.tables.pojos.HierarchyMetadata> hierarchyMetadata = getDslContext().selectFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.PARENT.eq(loginId))
                .fetchInto(com.salescode.dim.jooq.generated.tables.pojos.HierarchyMetadata.class);
        return hierarchyMetadata.stream().map(HierarchyMetadata::of).collect(Collectors.toList());

    }


}
