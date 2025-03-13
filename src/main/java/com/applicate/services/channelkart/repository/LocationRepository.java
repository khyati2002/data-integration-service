package com.applicate.services.channelkart.repository;

import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.jooq.impl.Location;
import org.jooq.DSLContext;

import static com.salescode.dim.jooq.generated.Tables.CK_LOCATION;

public class LocationRepository {
    private final DSLContext dsl;

    public LocationRepository(DSLContext dsl){
        this.dsl = dsl;
    }

    @Cacheable
    public Location findByLocationHierarchy(String locationHierarchy) {
        return dsl.selectFrom(CK_LOCATION)
                .where(CK_LOCATION.LOCATION_HIERARCHY.eq(locationHierarchy))
                .fetchOneInto(Location.class);
    }
}
