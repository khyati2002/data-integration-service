package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import com.salescode.dim.jooq.impl.ProductDetails;
import org.jooq.DSLContext;

import java.util.Optional;

import static com.salescode.dim.jooq.generated.Tables.CK_METADATA;
import static com.salescode.dim.jooq.generated.Tables.CK_PRODUCTDETAILS;


public class ProductDetailsRepository {

    private final DSLContext dsl;

    public ProductDetailsRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<ProductDetails> findByBacthCode(String batchCode){
        return dsl.selectFrom(CK_PRODUCTDETAILS)
                .where(CK_PRODUCTDETAILS.BATCH_CODE.eq(batchCode))
                .fetchOptionalInto(ProductDetails.class);
    }

}
