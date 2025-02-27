package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.HierarchyMetadata;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.salescode.dim.jooq.generated.Tables.*;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.select;

public class HierarchyMetadataRepository {
    private final DSLContext dsl;

    public HierarchyMetadataRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public HierarchyMetadata findByHierarchy(String hierarchy) {
        return dsl.selectFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.HIERARCHY.eq(hierarchy))
                .fetchOneInto(HierarchyMetadata.class);
    }

    
    public Collection<HierarchyMetadata> findByImmediateParent(String loginId) {
        return dsl.selectFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.PARENT.eq(loginId))
                .fetchInto(HierarchyMetadata.class);

    }

    
    public Collection<HierarchyMetadata> findByImmediateParentIn(List<String> loginId) {
        return dsl.selectFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.PARENT.in(loginId))
                .fetchInto(HierarchyMetadata.class);
    }

    public Collection<HierarchyMetadata> findBylocationHierarchy(String locationObj) {
        return dsl.selectFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.LOCATION_HIERARCHY.eq(locationObj))
                .fetchInto(HierarchyMetadata.class);
    }


    public List<HierarchyMetadata> findMyHierarchy(String loginId) {
        return dsl.selectFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.PARENT.in(
                        select(CK_USER_PARENT.PARENT)
                                .from(CK_USER_PARENT)
                                .where(CK_USER_PARENT.USERLOGINID.eq(loginId))
                ))
                .fetchInto(HierarchyMetadata.class);
    }


    public void updateHierarchy() {
        dsl.update(CK_HIERARCHY_METADATA)
                .set(CK_HIERARCHY_METADATA.HIERARCHY, CK_USER.HIERARCHY)
                .from(CK_USER)
                .where(CK_HIERARCHY_METADATA.PARENT.eq(CK_USER.LOGINID))
                .execute();
    }


    public void deleteHierarchyMetaData(String loginid) {
        dsl.deleteFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.PARENT.eq(loginid))
                .execute();
    }


    public void deleteByHierarchyIn(Collection<String> hierarchy) {
        dsl.deleteFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.HIERARCHY.in(hierarchy))
                .execute();
    }


    public void deleteByHierarchy(String hierarchy) {
        dsl.deleteFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.HIERARCHY.eq(hierarchy))
                .execute();
    }


    public void updateLocationHierarchy() {
        dsl.update(CK_HIERARCHY_METADATA)
                .set(CK_HIERARCHY_METADATA.LOCATION_HIERARCHY, CK_USER.LOCATION_HIERARCHY)
                .from(CK_USER)
                .where(CK_HIERARCHY_METADATA.PARENT.eq(CK_USER.LOGINID))
                .execute();
    }


    public List<HierarchyMetadata> findByParentMatchByHierarchy(String loginId, String hierarchyUser) {
        return dsl.selectFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.PARENT.eq(loginId))
                .and(field("MATCH (hierarchy) AGAINST ({0})", Boolean.class, hierarchyUser))
                .fetchInto(HierarchyMetadata.class);
    }


    public List<Map<String, Object>> getHierarchyDetailsByLoginId(List<String> parents) {
        return dsl.select(CK_HIERARCHY_METADATA.PARENT,
                        CK_HIERARCHY_METADATA.HIERARCHY.as("hmdHierarchy"),
                        CK_HIERARCHY_METADATA.ID)
                .from(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.PARENT.in(parents))
                .fetch()
                .intoMaps();
    }
}



