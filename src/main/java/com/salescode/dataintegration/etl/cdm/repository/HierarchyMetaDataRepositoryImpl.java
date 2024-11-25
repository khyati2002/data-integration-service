package com.salescode.dataintegration.etl.cdm.repository;

import com.salescode.jooq.generated.tables.pojos.CkHierarchyMetadata;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.salescode.jooq.generated.Tables.CK_USER;
import static com.salescode.jooq.generated.tables.CkHierarchyMetadata.CK_HIERARCHY_METADATA;
import static com.salescode.jooq.generated.tables.CkUserParent.CK_USER_PARENT;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.select;

@Repository
public class HierarchyMetaDataRepositoryImpl implements HierarchyMetaDataRepository {
    private final DSLContext dsl;

    public HierarchyMetaDataRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }
    @Override
    public CkHierarchyMetadata findByHierarchy(String hierarchy) {
       return dsl.selectFrom(CK_HIERARCHY_METADATA)
               .where(CK_HIERARCHY_METADATA.HIERARCHY.eq(hierarchy))
               .fetchOneInto(CkHierarchyMetadata.class);
    }

    @Override
    public Collection<CkHierarchyMetadata> findByImmediateParent(String loginId) {
     return dsl.selectFrom(CK_HIERARCHY_METADATA)
             .where(CK_HIERARCHY_METADATA.ID.eq(loginId))
             .fetchInto(CkHierarchyMetadata.class);

    }

    @Override
    public Collection<CkHierarchyMetadata> findByImmediateParentIn(List<String> loginId) {
        return dsl.selectFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.PARENT.in(loginId))
                .fetchInto(CkHierarchyMetadata.class);
    }

    @Override
    public Collection<CkHierarchyMetadata> findBylocationHierarchy(String locationObj) {
        return dsl.selectFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.LOCATION_HIERARCHY.eq(locationObj))
                .fetchInto(CkHierarchyMetadata.class);
    }

    @Override
    public List<CkHierarchyMetadata> findMyHierarchy(String loginId) {
        return dsl.selectFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.PARENT.in(
                        select(CK_USER_PARENT.PARENT)
                                .from(CK_USER_PARENT)
                                .where(CK_USER_PARENT.USERLOGINID.eq(loginId))
                ))
                .fetchInto(CkHierarchyMetadata.class);
    }

    @Override
    public void updateHierarchy() {
        dsl.update(CK_HIERARCHY_METADATA)
                .set(CK_HIERARCHY_METADATA.HIERARCHY, CK_USER.HIERARCHY)
                .from(CK_USER)
                .where(CK_HIERARCHY_METADATA.PARENT.eq(CK_USER.LOGINID))
                .execute();
    }

    @Override
    public void deleteHierarchyMetaData(String loginid) {
        dsl.deleteFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.PARENT.eq(loginid))
                .execute();
    }

    @Override
    public void deleteByHierarchyIn(Collection<String> hierarchy) {
        dsl.deleteFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.HIERARCHY.in(hierarchy))
                .execute();
    }

    @Override
    public void deleteByHierarchy(String hierarchy) {
        dsl.deleteFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.HIERARCHY.eq(hierarchy))
                .execute();
    }

    @Override
    public void updateLocationHierarchy() {
        dsl.update(CK_HIERARCHY_METADATA)
                .set(CK_HIERARCHY_METADATA.LOCATION_HIERARCHY, CK_USER.LOCATION_HIERARCHY)
                .from(CK_USER)
                .where(CK_HIERARCHY_METADATA.PARENT.eq(CK_USER.LOGINID))
                .execute();
    }

    @Override
    public List<CkHierarchyMetadata> findByParentMatchByHierarchy(String loginId, String hierarchyUser) {
        return dsl.selectFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.PARENT.eq(loginId))
                .and(field("MATCH (hierarchy) AGAINST ({0})", Boolean.class, hierarchyUser))
                .fetchInto(CkHierarchyMetadata.class);
    }

    @Override
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
