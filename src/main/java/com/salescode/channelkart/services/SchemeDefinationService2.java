package com.salescode.channelkart.services;
import com.salescode.channelkart.repository.SchemeDefinationRepo2;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.jooq.generated.Tables;
import com.salescode.jooq.generated.tables.pojos.*;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;

import static com.salescode.jooq.generated.Tables.*;

@Service
public class SchemeDefinationService2 extends AbstractCDMService<CkSchemeDefination> {
    private final DSLContext dsl;

    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(CkSchemeDefination.class);

    /** The repository. */
    private final SchemeDefinationRepo2 schemeDefinationRepo2;
    private SchemeProductBifurcationService schemeProductBifurcationService;
    private SchemeLocationBifurcationService schemeLocationBifurcationService;
    private SchemeOutletBifurcationService schemeOutletBifurcationService;
    private SchemeCalculationService schemeCalculationService;

    public SchemeDefinationService2(DSLContext dsl, SchemeDefinationRepo2 schemeDefinationRepo2) {
        this.dsl = dsl;
        this.schemeDefinationRepo2 = schemeDefinationRepo2;
    }

    public CkSchemeDefination findBySchemeId(String schemeId) {

        CkSchemeDefination schemeDefination = schemeDefinationRepo2.findBySchemeId(schemeId);
        if (schemeDefination == null ) {
            return null;
        }
        return schemeDefination;
    }
    @Override
    public CkSchemeDefination refresh(CkSchemeDefination scheme) {
        CkSchemeDefination schemeDefination = schemeDefinationRepo2.findBySchemeId(scheme.getSchemeId());
        return schemeDefination == null ? scheme : schemeDefination;
    }

    @Override
    public CkSchemeDefination save(CkSchemeDefination scheme) {
        List<CkSchemeProductBifurcations> schemeProductBifurcationsList =
                dsl.selectFrom(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS)
                        .where(CK_SCHEME_PRODUCT_BIFURCATIONS.SCHEME_ID.eq(scheme.getSchemeId()))
                        .fetchInto(CkSchemeProductBifurcations.class);
        List<CkSchemeOutletBifurcations> schemeOutletBifurcationsList =
                dsl.selectFrom(Tables.CK_SCHEME_OUTLET_BIFURCATIONS)
                        .where(CK_SCHEME_OUTLET_BIFURCATIONS.SCHEME_ID.eq(scheme.getSchemeId()))
                        .fetchInto(CkSchemeOutletBifurcations.class);
        List<CkSchemeLocationBifurcations> schemeLocationBifurcationsList =
                dsl.selectFrom(Tables.CK_SCHEME_LOCATION_BIFURCATIONS)
                        .where(CK_SCHEME_LOCATION_BIFURCATIONS.SCHEME_ID.eq(scheme.getSchemeId()))
                        .fetchInto(CkSchemeLocationBifurcations.class);
        CkSchemeCalculation schemeCalculationList =
                dsl.selectFrom(Tables.CK_SCHEME_CALCULATION)
                        .where(CK_SCHEME_CALCULATION.SCHEME_ID.eq(scheme.getSchemeId()))
                        .fetchOneInto(CkSchemeCalculation.class);

        if (!schemeProductBifurcationsList.isEmpty()) {
            List<CkSchemeProductBifurcations> refreshedData = schemeProductBifurcationService.refresh(schemeProductBifurcationsList);
            schemeProductBifurcationService.batchSave(refreshedData);
        }
        if (!schemeLocationBifurcationsList.isEmpty()) {
            List<CkSchemeLocationBifurcations> refreshedData = schemeLocationBifurcationService.refresh(schemeLocationBifurcationsList);
            schemeLocationBifurcationService.batchSave(refreshedData);
        }
        if (!schemeOutletBifurcationsList.isEmpty()) {
            List<CkSchemeOutletBifurcations> refreshedData = schemeOutletBifurcationService.refresh(schemeOutletBifurcationsList);
            schemeOutletBifurcationService.batchSave(refreshedData);
        }
//        if (!schemeCalculationList.isEmpty()) {
//            List<CkSchemeCalculation> refreshedData = schemeCalculationService.refresh(schemeCalculationList);
//            schemeCalculationService.batchSave(refreshedData);
//        }

//        CkSchemeDefination refreshedScheme = refresh(scheme);
//        return super.save(refreshedScheme);

        return scheme;
    }


}
