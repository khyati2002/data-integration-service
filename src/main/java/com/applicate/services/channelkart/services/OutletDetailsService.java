package com.applicate.services.channelkart.services;

import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.jooq.impl.OutletDetails;

import static com.salescode.dim.jooq.generated.Tables.CK_OUTLET_DETAILS;

public class OutletDetailsService extends AbstractCDMService<OutletDetails> {

    @Cacheable
    public OutletDetails findByOutletCode(String outletcode) {
        return getDslContext().select(CK_OUTLET_DETAILS.asterisk().except(CK_OUTLET_DETAILS.COORDINATE))
                .from(CK_OUTLET_DETAILS).where(CK_OUTLET_DETAILS.OUTLETCODE.eq(outletcode))
                .fetchOneInto(OutletDetails.class);
    }
}
