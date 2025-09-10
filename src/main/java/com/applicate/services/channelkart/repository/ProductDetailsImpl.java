package com.applicate.services.channelkart.repository;

import com.applicate.services.channelkart.services.AbstractCDMService;
import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;
import org.jooq.DSLContext;

import static com.salescode.dim.jooq.generated.Tables.CK_PRODUCTDETAILS;

public class ProductDetailsImpl extends AbstractCDMService<Productdetails> implements ProductDetailsRepo {
    private final DSLContext dsl=ProductDetailsImpl.getDslContext();

    @Override
    public Productdetails findByBatchCode(String batchCode) {
        return dsl.selectFrom(CK_PRODUCTDETAILS)
                .where(CK_PRODUCTDETAILS.BATCH_CODE.eq(batchCode))
                .fetchOneInto(Productdetails.class);
    }
}
