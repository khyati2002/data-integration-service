package com.salescode.channelkart.repository.impl;

import com.salescode.channelkart.repository.LocationRepository;
import com.salescode.jooq.generated.tables.pojos.CkLocation;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

import static com.salescode.jooq.generated.tables.CkLocation.CK_LOCATION;

@Repository
public class LocationRepositoryImpl implements LocationRepository {
    private final DSLContext dsl;

    public LocationRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public CkLocation findByLocationHierarchy(String locationHierarchy) {
        return dsl.selectFrom(CK_LOCATION)
                .where(CK_LOCATION.LOCATION_HIERARCHY.eq(locationHierarchy))
                .fetchOneInto(CkLocation.class);
    }

    @Override
    public List<CkLocation> findByLocationHierarchyIn(Set<String> locationHierarchy) {
        return dsl.selectFrom(CK_LOCATION) // Replace CK_LOCATION with your actual jOOQ table
                .where(CK_LOCATION.LOCATION_HIERARCHY.in(locationHierarchy))
                .fetchInto(CkLocation.class); // Map result to CkLocation POJO
    }

    @Override
    public CkLocation findBySalescodeId(String salescodeId) {
        return dsl.selectFrom(CK_LOCATION) // Replace CK_LOCATION with your actual jOOQ table
                .where(CK_LOCATION.SALESCODE_ID.eq(salescodeId))
                .fetchOneInto(CkLocation.class); // Map result to CkLocation POJO
    }
}
