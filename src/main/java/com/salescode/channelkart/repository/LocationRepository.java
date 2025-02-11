package com.salescode.channelkart.repository;

import com.salescode.jooq.generated.tables.pojos.CkLocation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface LocationRepository  {

	CkLocation findByLocationHierarchy(String locationHierarchy);

	List<CkLocation> findByLocationHierarchyIn(Set<String> locationHierarchy);

	CkLocation findBySalescodeId (String salescodeId);

}
