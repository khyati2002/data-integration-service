package com.applicate.services.channelkart.repository;


import com.applicate.services.channelkart.models.HierarchyMetaData;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import javax.transaction.Transactional;


import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository
public interface HierarchyMetaDataRepository extends CommonJpaRepository<HierarchyMetaData, String > {

	public HierarchyMetaData findByHierarchy(String hierarchy);
	
	public Collection<HierarchyMetaData> findByImmediateParent(String loginId);
	public Collection<HierarchyMetaData> findByImmediateParentIn(List<String> loginId);
	public Collection<HierarchyMetaData> findBylocationHierarchy(String locationObj);
	
	
	@Query(value = "SELECT * FROM ck_hierarchy_metadata ckm WHERE ckm.parent in (select parent from ck_user_parent where userloginid=:loginId)",
			  nativeQuery = true)
	public List<HierarchyMetaData> findMyHierarchy( @Param("loginId")String loginId);

	
	@Modifying
	@Transactional
	@Query(value= "update ck_hierarchy_metadata hm inner join ck_user u on hm.parent=u.loginid set hm.hierarchy = u.hierarchy",nativeQuery=true)
	public void updateHierarchy();
	
	@Modifying
	@Transactional
	@Query(value= "delete from ck_hierarchy_metadata where parent=?1",nativeQuery=true)
	public void deleteHierarchyMetaData(String loginid);
	
	@Modifying(clearAutomatically=true, flushAutomatically=true)
	public void deleteByHierarchyIn(Collection<String> hierarchy);
	
	@Modifying(clearAutomatically=true, flushAutomatically=true)
	public void deleteByHierarchy(String hierarchy);
	
	@Modifying
	@Query(value= "update ck_hierarchy_metadata hm left join ck_user u on hm.parent=u.loginid set hm.location_hierarchy = u.location_hierarchy",nativeQuery=true)
	public void updateLocationHierarchy();
	
	@Query(value = "SELECT * from ck_hierarchy_metadata where parent = ?1 and match hierarchy against (?2)", nativeQuery = true)
	List<HierarchyMetaData> findByParentMatchByHierarchy(String loginId, String hierarchyUser);

	@Query(nativeQuery = true,value = "select parent,hierarchy as hmdHierarchy,id from ck_hierarchy_metadata where parent in (?1)")
	List<Map<String,Object>> getHierarchyDetailsByLoginId(List<String> parents);

}
