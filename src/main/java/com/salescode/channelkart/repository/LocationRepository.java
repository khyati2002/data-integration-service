package com.salescode.channelkart.repository;

import com.salescode.channelkart.models.Location;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface LocationRepository extends CommonJpaRepository<Location, String> {
	
	Location findByLocationHierarchy(String locationHierarchy);
	
	List<Location> findByLocationHierarchyIn(Set<String> locationHierarchy);

	Location findBySalescodeId (String salescodeId);

}
