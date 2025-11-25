package com.salescode.dim.repository;

import com.salescode.dim.jooq.generated.tables.pojos.SchemeProductBifurcations;
import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_PRODUCT_BIFURCATIONS;

public class SchemeProductBifurcationRepoImpl implements SchemeProductBifurcationRepo{
    private final DSLContext dsl;

    public SchemeProductBifurcationRepoImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<SchemeProductBifurcations> findBySchemeId(String schemeId) {
        return dsl.selectFrom(CK_SCHEME_PRODUCT_BIFURCATIONS)
                .where(CK_SCHEME_PRODUCT_BIFURCATIONS.SCHEME_ID.eq(schemeId))
                .fetchInto(SchemeProductBifurcations.class);
    }
}
