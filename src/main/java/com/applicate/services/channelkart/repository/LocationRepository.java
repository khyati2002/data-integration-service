package com.applicate.services.channelkart.repository;

import com.applicate.services.channelkart.models.Location;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface LocationRepository extends CommonJpaRepository<Location, String> {
	
	Location findByLocationHierarchy(String locationHierarchy);
	
	List<Location> findByLocationHierarchyIn(Set<String> locationHierarchy);

	Location findBySalescodeId (String salescodeId);

}
