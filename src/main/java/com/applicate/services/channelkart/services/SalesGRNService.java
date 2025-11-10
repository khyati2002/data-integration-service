package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.impl.GRNInfo;

public class SalesGRNService extends AbstractCDMService<GRNInfo>{

    public void addNewEntry(GRNInfo newGrnInfo) {
        super.save(newGrnInfo);
    }

}
