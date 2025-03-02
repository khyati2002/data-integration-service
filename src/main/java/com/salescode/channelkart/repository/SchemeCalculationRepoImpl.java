package com.salescode.channelkart.repository;

import com.salescode.jooq.generated.Tables;
import com.salescode.jooq.generated.tables.pojos.CkSchemeCalculation;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static com.salescode.jooq.generated.Tables.CK_SCHEME_CALCULATION;

@Repository
public class SchemeCalculationRepoImpl implements SchemeCalculationRepo {
    private final DSLContext dsl;

    public SchemeCalculationRepoImpl(DSLContext dsl) {
        this.dsl = dsl;
    }
    @Override
    public CkSchemeCalculation findBySchemeId(String schemeId) {
        return dsl.selectFrom(Tables.CK_SCHEME_CALCULATION).where(CK_SCHEME_CALCULATION.SCHEME_ID.eq(schemeId))
                .fetchOneInto(CkSchemeCalculation.class);
    }
}
