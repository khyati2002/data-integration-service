package com.salescode.dim.repository;

import com.salescode.dim.jooq.generated.tables.CkSchemeLocationBifurcations;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeLocationBifurcations;
import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_LOCATION_BIFURCATIONS;

public class SchemeLocationBifurcationRepoImpl implements SchemeLocationBifurcationRepo{

    private final DSLContext dsl;
    public SchemeLocationBifurcationRepoImpl(DSLContext dsl) {
        this.dsl = dsl;
    }
    @Override
    public List<SchemeLocationBifurcations> findBySchemeId(String schemeId) {
        return dsl.selectFrom(CK_SCHEME_LOCATION_BIFURCATIONS)
                .where(CK_SCHEME_LOCATION_BIFURCATIONS.SCHEME_ID.eq(schemeId))
                .fetchInto(SchemeLocationBifurcations.class);
    }

}
