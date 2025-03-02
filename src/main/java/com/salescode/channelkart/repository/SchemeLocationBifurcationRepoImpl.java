package com.salescode.channelkart.repository;

import com.salescode.jooq.generated.Tables;
import com.salescode.jooq.generated.tables.pojos.CkSchemeLocationBifurcations;
import com.salescode.jooq.generated.tables.pojos.CkSchemeOutletBifurcations;
import org.jooq.DSLContext;

import static com.salescode.jooq.generated.Tables.CK_SCHEME_LOCATION_BIFURCATIONS;
import static com.salescode.jooq.generated.Tables.CK_SCHEME_OUTLET_BIFURCATIONS;

public class SchemeLocationBifurcationRepoImpl implements SchemeLocationBifurcationRepo{
    private final DSLContext dsl;
    public SchemeLocationBifurcationRepoImpl(DSLContext dsl) {
        this.dsl = dsl;
    }
    @Override
    public CkSchemeLocationBifurcations findBySchemeId(String schemeId) {
        return dsl.selectFrom(CK_SCHEME_LOCATION_BIFURCATIONS)
                .where(CK_SCHEME_LOCATION_BIFURCATIONS.SCHEME_ID.eq(schemeId))
                .fetchOneInto(CkSchemeLocationBifurcations.class);
    }
}
