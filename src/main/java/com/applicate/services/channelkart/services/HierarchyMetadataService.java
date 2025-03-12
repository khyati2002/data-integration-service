package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.impl.HierarchyMetadata;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_HIERARCHY_METADATA;

public class HierarchyMetadataService extends AbstractCDMService<HierarchyMetadata> {

    public List<HierarchyMetadata> findByImmediateParent(String loginId) {
        return getDslContext().selectFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.PARENT.eq(loginId))
                .fetchInto(HierarchyMetadata.class);

    }


}
