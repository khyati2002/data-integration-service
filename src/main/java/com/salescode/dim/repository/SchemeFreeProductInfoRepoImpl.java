package com.salescode.dim.repository;

import com.salescode.dim.jooq.generated.tables.pojos.SchemeFreeproductinfo;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeLocationBifurcations;
import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_FREEPRODUCTINFO;
import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_LOCATION_BIFURCATIONS;

public class SchemeFreeProductInfoRepoImpl implements SchemeFreeProductInfoRepo{
    private final DSLContext dsl;
    public SchemeFreeProductInfoRepoImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<SchemeFreeproductinfo> findBySchemeId(String schemeId) {
        return dsl.selectFrom(CK_SCHEME_FREEPRODUCTINFO)
                .where(CK_SCHEME_FREEPRODUCTINFO.SCHEME_ID.eq(schemeId))
                .fetchInto(SchemeFreeproductinfo.class);
    }
}
