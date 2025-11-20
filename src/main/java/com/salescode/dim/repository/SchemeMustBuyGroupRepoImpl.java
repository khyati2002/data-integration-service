package com.salescode.dim.repository;

import com.salescode.dim.jooq.generated.tables.pojos.SchemeMustBuyGroup;
import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_MUST_BUY_GROUP;

public class SchemeMustBuyGroupRepoImpl implements SchemeMustBuyGroupRepo{
    private final DSLContext dsl;

    public SchemeMustBuyGroupRepoImpl(DSLContext dsl) {
        this.dsl = dsl;
    }
    @Override
    public List<SchemeMustBuyGroup> findBySchemeId(String schemeId) {
        return dsl.selectFrom(CK_SCHEME_MUST_BUY_GROUP)
                .where(CK_SCHEME_MUST_BUY_GROUP.SCHEME_ID.eq(schemeId))
                .fetchInto(SchemeMustBuyGroup.class);
    }
}
