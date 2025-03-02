package com.salescode.channelkart.services;

import com.salescode.channelkart.repository.SchemeLocationBifurcationRepo;
import com.salescode.channelkart.repository.SchemeProductBifurcationRepo;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.jooq.generated.tables.pojos.CkSchemeLocationBifurcations;
import com.salescode.jooq.generated.tables.pojos.CkSchemeProductBifurcations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemeLocationBifurcationService extends AbstractCDMService<CkSchemeLocationBifurcations> {
    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(SchemeLocationBifurcationService.class);

    /** The repository. */
    private SchemeLocationBifurcationRepo schemeLocationBifurcationRepo;
    public CkSchemeLocationBifurcations findBySchemeId(String schemeId) {

        CkSchemeLocationBifurcations locationBifurcations = schemeLocationBifurcationRepo.findBySchemeId(schemeId);
        if (locationBifurcations == null ) {
            return null;
        }
        return locationBifurcations;
    }
    @Override
    public CkSchemeLocationBifurcations refresh(CkSchemeLocationBifurcations scheme) {
        CkSchemeLocationBifurcations schemeLocationBifurcations = schemeLocationBifurcationRepo.findBySchemeId(scheme.getSchemeId());
        return schemeLocationBifurcations == null ? scheme : schemeLocationBifurcations;
    }
    @Override
    public CkSchemeLocationBifurcations save(CkSchemeLocationBifurcations scheme) {
        CkSchemeLocationBifurcations refreshedScheme = refresh(scheme);
        return super.save(refreshedScheme);
    }
}
