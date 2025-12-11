package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.HierarchyMetadata;
import org.jooq.DSLContext;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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

    public List<HierarchyMetadata> findByHierarchyIn(Set<String> hierarchyStrings) {
        if (hierarchyStrings == null || hierarchyStrings.isEmpty()) {
            return Collections.emptyList();
        }

        return dsl
                .select()
                .from(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.HIERARCHY.in(hierarchyStrings))
                .fetchInto(HierarchyMetadata.class);
    }

}
