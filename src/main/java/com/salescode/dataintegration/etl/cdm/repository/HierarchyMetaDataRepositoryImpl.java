package com.salescode.dataintegration.etl.cdm.repository;

import com.salescode.jooq.generated.tables.pojos.CkHierarchyMetadata;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.salescode.jooq.generated.tables.CkHierarchyMetadata.CK_HIERARCHY_METADATA;
import static com.salescode.jooq.generated.tables.CkUserParent.CK_USER_PARENT;
import static org.jooq.impl.DSL.select;

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

    }

    @Override
    public Collection<CkHierarchyMetadata> findByImmediateParentIn(List<String> loginId) {

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

    }

    @Override
    public void deleteHierarchyMetaData(String loginid) {

    }

    @Override
    public void deleteByHierarchyIn(Collection<String> hierarchy) {

    }

    @Override
    public void deleteByHierarchy(String hierarchy) {

    }

    @Override
    public void updateLocationHierarchy() {

    }

    @Override
    public List<CkHierarchyMetadata> findByParentMatchByHierarchy(String loginId, String hierarchyUser) {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getHierarchyDetailsByLoginId(List<String> parents) {
        return List.of();
    }
}
