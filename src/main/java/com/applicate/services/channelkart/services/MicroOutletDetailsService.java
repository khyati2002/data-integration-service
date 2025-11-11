package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.impl.MicroOutletDetails;

import java.util.UUID;

import static com.salescode.dim.jooq.generated.Tables.CK_OUTLET_DETAILS;
import static java.lang.System.out;


public class MicroOutletDetailsService extends AbstractCDMService<MicroOutletDetails> {
    public static final String CACHE_DOMAIN = "microOutlets";

//    @Override
//    public MicroOutletDetails save(MicroOutletDetails cdmObject) {
//        MicroOutletDetails save = super.save(cdmObject);
//        return save;
//    }

    public MicroOutletDetails save(MicroOutletDetails cdmObject) {
        if (cdmObject.getId() == null) {
            cdmObject.setId(UUID.randomUUID().toString());
        }

        fillCommonAttributes(cdmObject);

        var record = getDslContext().newRecord(CK_OUTLET_DETAILS, cdmObject);

        System.out.println(record.getOutletcode());

        record.changed(CK_OUTLET_DETAILS.SCODE, false);
        record.store();
        return record.into(MicroOutletDetails.class);
    }
    public MicroOutletDetails findByOutletCode(String outletCode) {
        return getDslContext()
                .selectFrom(CK_OUTLET_DETAILS)
                .where(CK_OUTLET_DETAILS.OUTLETCODE.eq(outletCode))
                .fetchOneInto(MicroOutletDetails.class);
    }



}
