/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.repository;


import com.salescode.dim.jooq.impl.Tax;
import org.jooq.DSLContext;
import static com.salescode.dim.jooq.generated.Tables.CK_TAX;


import java.util.List;


public class TaxRepository {
    private final DSLContext dsl;

    public TaxRepository(DSLContext dsl) {
        this.dsl = dsl;
    }
	
	public List<Tax> findByBatchCodeIn(List<String> batchCodeList){
        return dsl.selectFrom(CK_TAX)
                .where(CK_TAX.BATCH_CODE.in(batchCodeList))
                .fetchInto(Tax.class);
    }

}
