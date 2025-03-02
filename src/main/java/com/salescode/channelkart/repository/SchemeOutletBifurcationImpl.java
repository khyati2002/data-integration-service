package com.salescode.channelkart.repository;

import com.salescode.jooq.generated.Tables;
import com.salescode.jooq.generated.tables.pojos.CkSchemeDefination;
import com.salescode.jooq.generated.tables.pojos.CkSchemeOutletBifurcations;
import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.jooq.generated.Tables.CK_SCHEME_DEFINATION;
import static com.salescode.jooq.generated.Tables.CK_SCHEME_OUTLET_BIFURCATIONS;

public class SchemeOutletBifurcationImpl implements SchemeOutletBifurcationRepo {
    private final DSLContext dsl;
    public SchemeOutletBifurcationImpl(DSLContext dsl) {
        this.dsl = dsl;
    }
    @Override
    public CkSchemeOutletBifurcations findBySchemeId(String schemeId) {
        return dsl.selectFrom(Tables.CK_SCHEME_OUTLET_BIFURCATIONS)
                .where(CK_SCHEME_OUTLET_BIFURCATIONS.SCHEME_ID.eq(schemeId))
                .fetchOneInto(CkSchemeOutletBifurcations.class);
    }
}
