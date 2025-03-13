package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.generated.tables.pojos.ChannelHierarchyMetadata;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.jooq.DSLContext;

import java.util.*;

public class SupplierInfoService {
    private static ChannelHierarchyMetadataService channelHierarchyService;
    private final DSLContext dsl;
    SupplierInfoService(DSLContext dsl){
        channelHierarchyService = new ChannelHierarchyMetadataService(dsl);
        this.dsl = dsl;
    }
    public List<String> findSuppliers(OutletDetails outlet) {
        if(outlet != null) {
            Collection<ChannelHierarchyMetadata> parent= channelHierarchyService.getOutletChannelHierarchy(outlet);
            Set<String> suppliers= new HashSet<>();
            for (ChannelHierarchyMetadata data : parent){
                if(data.getLevel1supplier() != null)
                    suppliers.add(data.getLevel1supplier());
                if(data.getLevel2supplier() != null)
                    suppliers.add(data.getLevel2supplier());
                if(data.getLevel3supplier() != null)
                    suppliers.add(data.getLevel3supplier());
            }
            return new ArrayList<>(suppliers);
        }else {
      //      logger.error("Cannot find outlet for fetching suppliers");
        }

        return List.of();
    }
}
