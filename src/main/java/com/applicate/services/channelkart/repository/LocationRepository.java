package com.applicate.services.channelkart.repository;

import com.applicate.services.channelkart.cache.DistributedCache;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.cache.CacheKeys;
import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.jooq.impl.Location;
import org.jooq.DSLContext;

import java.util.function.Function;

import static com.salescode.dim.jooq.generated.Tables.CK_LOCATION;

public class LocationRepository {
    private final DSLContext dsl;
    private final DistributedCache distributedCache;;

    public LocationRepository(DSLContext dsl){
        this.distributedCache = DistributedCache.getInstance();
        this.dsl = dsl;
    }

    public Location findByLocationHierarchy(String locationHierarchy, boolean cache) {
        if (locationHierarchy == null) {
            return null;
        }
        Function<String, Location> loader = (String lh) -> dsl.selectFrom(CK_LOCATION).where(CK_LOCATION.LOCATION_HIERARCHY.eq(lh)).fetchOneInto(Location.class);
        return cache ? distributedCache.withCache(SecurityContextUtils.getLob(), CacheKeys.LOCATION_CACHE_DOMAIN, locationHierarchy, loader) : loader.apply(locationHierarchy);
    }

}
