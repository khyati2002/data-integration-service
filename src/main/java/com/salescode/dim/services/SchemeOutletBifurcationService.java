package com.salescode.dim.services;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.AbstractCDMService;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeOutletBifurcations;
import org.jooq.impl.DSL;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_OUTLET_BIFURCATIONS;

public class SchemeOutletBifurcationService extends AbstractCDMService<SchemeOutletBifurcations> {

    public void expireBudget(String promoCode, String supplier) {
        getDslContext().update(CK_SCHEME_OUTLET_BIFURCATIONS)
                .set(CK_SCHEME_OUTLET_BIFURCATIONS.ACTIVE_STATUS, ActiveStatus.INACTIVE)
                .set(CK_SCHEME_OUTLET_BIFURCATIONS.LAST_MODIFIED_TIME, DSL.currentLocalDateTime())
                .where(CK_SCHEME_OUTLET_BIFURCATIONS.SCHEME_ID.eq(promoCode))
                .and(CK_SCHEME_OUTLET_BIFURCATIONS.LOGIN_ID.eq(supplier))
                .execute();
    }
}