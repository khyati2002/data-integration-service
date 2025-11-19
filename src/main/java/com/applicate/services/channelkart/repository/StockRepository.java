package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.Stock;
import org.jooq.DSLContext;

import java.util.Map;
import java.util.Optional;

import static com.salescode.dim.jooq.generated.Tables.CK_STOCK;

public class StockRepository {

    private final DSLContext dsl;

    public StockRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<Stock> findBySkuCodeAndSupplier(String skuCode, String supplier) {
        return dsl.selectFrom(CK_STOCK)
                .where(CK_STOCK.SKU_CODE.eq(skuCode))
                .and(CK_STOCK.SUPPLIER.eq(supplier))
                .fetchOptionalInto(Stock.class);
    }


}