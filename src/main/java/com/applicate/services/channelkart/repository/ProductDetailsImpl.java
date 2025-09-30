package com.applicate.services.channelkart.repository;

import com.applicate.services.channelkart.services.AbstractCDMService;
import com.salescode.dim.jooq.generated.Tables;
import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;
import org.jooq.DSLContext;

import java.util.List;

public class ProductDetailsImpl
        extends AbstractCDMService<Productdetails>
        implements ProductDetailsRepo {
    private final DSLContext dsl = ProductDetailsImpl.getDslContext();

    @Override
    public Productdetails findByBatchCode(String batchCode) {
        return this.dsl.selectFrom(Tables.CK_PRODUCTDETAILS).where(Tables.CK_PRODUCTDETAILS.BATCH_CODE.eq(batchCode)).fetchOneInto(Productdetails.class);
    }

    @Override
    public List<Productdetails> findByEanCode(String eanCode) {
        return this.dsl.selectFrom(Tables.CK_PRODUCTDETAILS).where(Tables.CK_PRODUCTDETAILS.EAN_NUMBER.eq(eanCode)).fetchInto(Productdetails.class);
    }
}