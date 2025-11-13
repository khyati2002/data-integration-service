package com.applicate.services.channelkart.repository;
import org.jooq.DSLContext;

import java.util.Optional;

import static com.salescode.dim.jooq.generated.Tables.CK_OUTLET_METADATA;


public class OutletMetaDataRepository {

    private final DSLContext dsl;

    public OutletMetaDataRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<String> getOutletCodeIfExists(String outletcode){
        return dsl.selectFrom(CK_OUTLET_METADATA) // Replace CK_METADATA with your actual jOOQ table
                .where(CK_OUTLET_METADATA.OUTLET_CODE.eq(outletcode))
                .fetchOptional()
                .map(record -> record.getValue(CK_OUTLET_METADATA.OUTLET_CODE));
    }

}
