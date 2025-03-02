package com.salescode.channelkart.repository;
import com.salescode.jooq.generated.Tables;
import com.salescode.jooq.generated.tables.pojos.CkSchemeProductBifurcations;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.salescode.jooq.generated.Tables.CK_SCHEME_OUTLET_BIFURCATIONS;
import static com.salescode.jooq.generated.Tables.CK_SCHEME_PRODUCT_BIFURCATIONS;

@Repository
public class SchemeProductBifurcationRepoImpl implements SchemeProductBifurcationRepo {
    private final DSLContext dsl;

    public SchemeProductBifurcationRepoImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public CkSchemeProductBifurcations findBySchemeId(String schemeId) {
        return dsl.selectFrom(CK_SCHEME_PRODUCT_BIFURCATIONS)
                .where(CK_SCHEME_PRODUCT_BIFURCATIONS.SCHEME_ID.eq(schemeId))
                .fetchOneInto(CkSchemeProductBifurcations.class);
    }
}
