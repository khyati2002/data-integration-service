package com.salescode.channelkart.repository.impl;

import com.salescode.channelkart.repository.OutletDetailsRepository;
import com.salescode.jooq.generated.tables.pojos.CkOutletDetails;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static com.salescode.jooq.generated.Tables.CK_OUTLET_DETAILS;

@Repository
public class OutletDetailsRepositoryImpl implements OutletDetailsRepository {
    private final DSLContext dsl;

    public OutletDetailsRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public CkOutletDetails findByOutletCode(String outletCode) {
        return dsl.selectFrom(CK_OUTLET_DETAILS)
                .where(CK_OUTLET_DETAILS.OUTLETCODE.eq(outletCode))
                .fetchOneInto(CkOutletDetails.class);
    }
}
