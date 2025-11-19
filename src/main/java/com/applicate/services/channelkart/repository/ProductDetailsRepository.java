package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;
import org.jooq.DSLContext;
import org.jooq.SelectFieldOrAsterisk;

import java.util.Collection;
import java.util.Map;

import static com.salescode.dim.jooq.generated.Tables.CK_PRODUCTDETAILS;

public class ProductDetailsRepository {

    private final DSLContext dsl;

    public ProductDetailsRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public boolean existsByBatchCode(String batchCode) {
        return dsl.fetchExists(
                dsl.selectFrom(CK_PRODUCTDETAILS)
                        .where(CK_PRODUCTDETAILS.BATCH_CODE.eq(batchCode))
        );
    }
    public Productdetails findByBatchCode(String batchCode) {
        return dsl.selectFrom( CK_PRODUCTDETAILS)
                        .where(CK_PRODUCTDETAILS.BATCH_CODE.eq(batchCode)).fetchOneInto(Productdetails.class);
    }
}