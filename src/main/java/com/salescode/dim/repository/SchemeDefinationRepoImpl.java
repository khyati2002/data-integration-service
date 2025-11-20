package com.salescode.dim.repository;

import com.salescode.dim.jooq.impl.SchemeDefination;
import org.jooq.DSLContext;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_DEFINATION;

public class SchemeDefinationRepoImpl implements SchemeDefinationRepo{
    private final DSLContext dsl;
    public SchemeDefinationRepoImpl(DSLContext dsl) {
        this.dsl = dsl;
    }
    @Override
    public SchemeDefination findBySchemeId(String schemeId){
        return dsl.selectFrom(CK_SCHEME_DEFINATION)
                .where(CK_SCHEME_DEFINATION.SCHEME_ID.eq(schemeId))
                .fetchOneInto(SchemeDefination.class);
    }
}
