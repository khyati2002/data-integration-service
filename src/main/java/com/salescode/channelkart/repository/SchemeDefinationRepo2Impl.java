package com.salescode.channelkart.repository;
import com.salescode.jooq.generated.Tables;
import com.salescode.jooq.generated.tables.pojos.CkSchemeDefination;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static com.salescode.jooq.generated.Tables.CK_SCHEME_DEFINATION;

@Repository
public class SchemeDefinationRepo2Impl implements SchemeDefinationRepo2 {
    private final DSLContext dsl;
    public SchemeDefinationRepo2Impl(DSLContext dsl) {
        this.dsl = dsl;
    }
    @Override
    public CkSchemeDefination findBySchemeId(String schemeId){
        return dsl.selectFrom(Tables.CK_SCHEME_DEFINATION)
                .where(CK_SCHEME_DEFINATION.SCHEME_ID.eq(schemeId))
                .fetchOneInto(CkSchemeDefination.class);
    }

}
