package com.salescode.channelkart.services;

import com.salescode.channelkart.repository.SchemeOutletBifurcationRepo;
import com.salescode.channelkart.repository.SchemeProductBifurcationRepo;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.jooq.generated.tables.pojos.CkSchemeOutletBifurcations;
import com.salescode.jooq.generated.tables.pojos.CkSchemeProductBifurcations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemeOutletBifurcationService extends AbstractCDMService<CkSchemeOutletBifurcations> {
    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(SchemeOutletBifurcationService.class);

    /** The repository. */
    private SchemeOutletBifurcationRepo schemeOutletBifurcationRepo;
    public CkSchemeOutletBifurcations findBySchemeId(String schemeId) {

        CkSchemeOutletBifurcations outletBifurcation = schemeOutletBifurcationRepo.findBySchemeId(schemeId);
        if (outletBifurcation == null ) {
            return null;
        }
        return outletBifurcation;

    }
    @Override
    public CkSchemeOutletBifurcations refresh(CkSchemeOutletBifurcations scheme) {
        CkSchemeOutletBifurcations schemeOutletBifurcations = schemeOutletBifurcationRepo.findBySchemeId(scheme.getSchemeId());
        return schemeOutletBifurcations == null ? scheme : schemeOutletBifurcations;
    }

    @Override
    public CkSchemeOutletBifurcations save(CkSchemeOutletBifurcations scheme) {
        CkSchemeOutletBifurcations refreshedScheme = refresh(scheme);
        return super.save(refreshedScheme);
    }
}
