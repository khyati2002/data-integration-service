package com.salescode.channelkart.repository;

import com.salescode.jooq.generated.tables.pojos.CkHierarchyMetadata;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository
public interface HierarchyMetaDataRepository {

    public CkHierarchyMetadata findByHierarchy(String hierarchy);

    public Collection<CkHierarchyMetadata> findByImmediateParent(String loginId);

    public Collection<CkHierarchyMetadata> findByImmediateParentIn(List<String> loginId);

    public Collection<CkHierarchyMetadata> findBylocationHierarchy(String locationObj);

    public List<CkHierarchyMetadata> findMyHierarchy(String loginId);

    public void updateHierarchy();

    public void deleteHierarchyMetaData(String loginid);

    public void deleteByHierarchyIn(Collection<String> hierarchy);

    public void deleteByHierarchy(String hierarchy);

    public void updateLocationHierarchy();

    List<CkHierarchyMetadata> findByParentMatchByHierarchy(String loginId, String hierarchyUser);

    List<Map<String, Object>> getHierarchyDetailsByLoginId(List<String> parents);

}
