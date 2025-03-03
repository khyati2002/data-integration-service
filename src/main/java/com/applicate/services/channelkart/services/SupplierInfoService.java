package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.impl.OutletDetails;

import java.util.*;

public class SupplierInfoService {
    public List<String> findSuppliers(OutletDetails outlet) {
//        if(outlet != null) {
//            Collection<ChannelHierarchyMetaData> parent= channelHierarchyService.getOutletChannelHierarchy(outlet);
//            Set<String> suppliers= new HashSet<>();
//            for (ChannelHierarchyMetaData data : parent){
//                if(data.getLevel1Supplier() != null)
//                    suppliers.add(data.getLevel1Supplier());
//                if(data.getLevel2Supplier() != null)
//                    suppliers.add(data.getLevel2Supplier());
//                if(data.getLevel3Supplier() != null)
//                    suppliers.add(data.getLevel3Supplier());
//            }
//            return new ArrayList<>(suppliers);
//        }else {
//            logger.error("Cannot find outlet for fetching suppliers");
//        }

        return List.of();
    }
}
