package com.applicate.services.channelkart.repository_new;

import com.salescode.dim.jooq.impl.HierarchyMetadata;
import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_HIERARCHY_METADATA;

public class HierarchyMetadataRepository {
    private final DSLContext dsl;

    public HierarchyMetadataRepository(DSLContext dsl){
        this.dsl = dsl;
    }

    public List<HierarchyMetadata> findByImmediateParent(String loginId) {
        return dsl.selectFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.PARENT.eq(loginId))
                .fetchInto(HierarchyMetadata.class);

    }

    public HierarchyMetadata findByHierarchy(String hierarchy) {
        return dsl.selectFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.HIERARCHY.eq(hierarchy))
                .fetchOneInto(HierarchyMetadata.class);
    }

}
