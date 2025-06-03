package com.applicate.services.channelkart.repository;
import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;
import org.jooq.DSLContext;
import static com.salescode.dim.jooq.generated.Tables.CK_PRODUCTDETAILS;
public class ProductDetailsImpl implements ProductDetailsRepo {
    private final DSLContext dsl;
    public ProductDetailsImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Productdetails findByBatchCode(String batchCode) {
        return dsl.selectFrom(CK_PRODUCTDETAILS)
                .where(CK_PRODUCTDETAILS.BATCH_CODE.eq(batchCode))
                .fetchOneInto(Productdetails.class);
    }
}
