package com.salescode.dim.services;

import com.applicate.services.channelkart.services.AbstractCDMService;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeCalculation;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_CALCULATION;

public class SchemeCalculationService extends AbstractCDMService<SchemeCalculation> {

    public String getSchemeType(String promoCode) {
        return getDslContext().select(CK_SCHEME_CALCULATION.SCHEME_TYPE)
                .from(CK_SCHEME_CALCULATION)
                .where(CK_SCHEME_CALCULATION.SCHEME_ID.eq(promoCode))
                .fetchOneInto(String.class);
    }


}