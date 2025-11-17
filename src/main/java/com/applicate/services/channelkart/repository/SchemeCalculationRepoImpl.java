package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.SchemeCalculation;
import org.jooq.DSLContext;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_CALCULATION;

public class SchemeCalculationRepoImpl implements SchemeCalculationRepo{
    private final DSLContext dsl;

    public SchemeCalculationRepoImpl(DSLContext dsl) {
        this.dsl = dsl;
    }
    @Override
    public SchemeCalculation findBySchemeId(String schemeId) {
        return dsl.selectFrom(CK_SCHEME_CALCULATION).where(CK_SCHEME_CALCULATION.SCHEME_ID.eq(schemeId))
                .fetchOneInto(SchemeCalculation.class);
    }
}
