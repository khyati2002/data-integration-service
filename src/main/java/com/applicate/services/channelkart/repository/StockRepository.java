package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.Stock;
import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_STOCK;

public class StockRepository {

    private final DSLContext dsl;

    public StockRepository(DSLContext dsl){
        this.dsl = dsl;
    }

    public List<Stock> findBySkuCodeInAndSupplier(List<String> skuCode, String supplier) {
        return dsl.selectFrom(CK_STOCK)
                .where(CK_STOCK.SKU_CODE.in(skuCode)
                        .and(CK_STOCK.SUPPLIER.eq(supplier)))
                .fetchInto(Stock.class);
    }


}