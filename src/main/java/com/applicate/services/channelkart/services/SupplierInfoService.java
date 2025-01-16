/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.services;


import com.applicate.services.channelkart.cache.DistributedCache;
import com.applicate.services.channelkart.models.ChannelHierarchyMetaData;
import com.applicate.services.channelkart.models.OutletDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * The class SupplierInfoService.
 *
 * @author Manish Srivastava
 * @since Aug 2020
 */
@Service
public class SupplierInfoService {

    /**
     * The cache domain.
     */
    private static final String CACHE_DOMAIN = "suppliers";
    private static final Logger logger = LoggerFactory.getLogger(SupplierInfoService.class);
    /**
     * The user service.
     */
    @Autowired
    private ChannelHierarchyMetaDataService channelHierarchyService;

    @Autowired
    private DistributedCache distributedCache;

    public List<String> findSuppliers(OutletDetails outlet) {
        if (outlet != null) {
            Collection<ChannelHierarchyMetaData> parent = channelHierarchyService.getOutletChannelHierarchy(outlet);
            Set<String> suppliers = new HashSet<>();
            for (ChannelHierarchyMetaData data : parent) {
                if (data.getLevel1Supplier() != null) suppliers.add(data.getLevel1Supplier());
                if (data.getLevel2Supplier() != null) suppliers.add(data.getLevel2Supplier());
                if (data.getLevel3Supplier() != null) suppliers.add(data.getLevel3Supplier());
            }
            return new ArrayList<>(suppliers);
        } else {
            logger.error("Cannot find outlet for fetching suppliers");
        }
        return List.of();
    }

    public void clearCache(String lob, String cacheKey) {
        distributedCache.clearCache(lob, CACHE_DOMAIN, cacheKey);
    }


}
