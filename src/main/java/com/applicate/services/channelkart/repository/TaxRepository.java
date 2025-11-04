/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.repository;


import com.salescode.dim.jooq.impl.Tax;
import org.jooq.DSLContext;


import java.util.List;
import static com.salescode.dim.jooq.generated.tables.CkTax.CK_TAX;


public class TaxRepository {

    private final DSLContext dslContext;

    public TaxRepository(DSLContext dslContext){
        this.dslContext=dslContext;
    }
	
	public List<Tax> findByBatchCodeIn(List<String> batchCodeList){
        return dslContext.selectFrom(CK_TAX)
                .where(CK_TAX.BATCH_CODE.in(batchCodeList))
                .fetchInto(Tax.class);
    }


}
