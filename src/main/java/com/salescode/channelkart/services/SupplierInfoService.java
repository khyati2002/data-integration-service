/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.services;


import com.salescode.jooq.generated.tables.pojos.CkChannelHierarchyMetadata;
import com.salescode.jooq.generated.tables.pojos.CkOutletDetails;
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

    public List<String> findSuppliers(CkOutletDetails outlet) {
        if (outlet != null) {
            Collection<CkChannelHierarchyMetadata> parent = channelHierarchyService.getOutletChannelHierarchy(outlet);
            Set<String> suppliers = new HashSet<>();
            for (CkChannelHierarchyMetadata data : parent) {
                if (data.getLevel1supplier() != null) suppliers.add(data.getLevel1supplier());
                if (data.getLevel2supplier() != null) suppliers.add(data.getLevel2supplier());
                if (data.getLevel3supplier() != null) suppliers.add(data.getLevel3supplier());
            }
            return new ArrayList<>(suppliers);
        } else {
            logger.error("Cannot find outlet for fetching suppliers");
        }
        return List.of();
    }


}
