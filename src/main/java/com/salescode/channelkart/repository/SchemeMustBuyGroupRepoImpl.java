package com.salescode.channelkart.repository;
import com.salescode.jooq.generated.tables.pojos.CkSchemeMustBuyGroup;
import org.jooq.DSLContext;
import static com.salescode.jooq.generated.Tables.CK_SCHEME_MUST_BUY_GROUP;

public class SchemeMustBuyGroupRepoImpl implements SchemeMustBuyGroupRepo{
    private final DSLContext dsl;

    public SchemeMustBuyGroupRepoImpl(DSLContext dsl) {
        this.dsl = dsl;
    }
    @Override
    public CkSchemeMustBuyGroup findBySchemeId(String schemeId) {
        return dsl.selectFrom(CK_SCHEME_MUST_BUY_GROUP)
                .where(CK_SCHEME_MUST_BUY_GROUP.SCHEME_ID.eq(schemeId))
                .fetchOneInto(CkSchemeMustBuyGroup.class);
    }

}
