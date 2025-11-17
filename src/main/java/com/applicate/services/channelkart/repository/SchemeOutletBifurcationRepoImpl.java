package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.SchemeOutletBifurcations;
import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_OUTLET_BIFURCATIONS;

public class SchemeOutletBifurcationRepoImpl implements SchemeOutletBifurcationRepo{
    private final DSLContext dsl;
    public SchemeOutletBifurcationRepoImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<SchemeOutletBifurcations> findBySchemeId(String schemeId) {
        return dsl.selectFrom(CK_SCHEME_OUTLET_BIFURCATIONS)
                .where(CK_SCHEME_OUTLET_BIFURCATIONS.SCHEME_ID.eq(schemeId))
                .fetchInto(SchemeOutletBifurcations.class);
    }
}
