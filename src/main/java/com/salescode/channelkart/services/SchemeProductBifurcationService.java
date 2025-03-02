package com.salescode.channelkart.services;
import com.salescode.channelkart.repository.SchemeProductBifurcationRepo;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.jooq.generated.tables.pojos.CkSchemeProductBifurcations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemeProductBifurcationService extends AbstractCDMService<CkSchemeProductBifurcations> {

    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(SchemeProductBifurcationService.class);

    /** The repository. */
    private SchemeProductBifurcationRepo schemeProductBifurcationRepo;
    public CkSchemeProductBifurcations findBySchemeId(String schemeId) {

        CkSchemeProductBifurcations productBifurcation = schemeProductBifurcationRepo.findBySchemeId(schemeId);
        if (productBifurcation == null ) {
            return null;
        }
        return productBifurcation;

    }
    @Override
    public CkSchemeProductBifurcations refresh(CkSchemeProductBifurcations scheme) {
        CkSchemeProductBifurcations schemeProductBifurcations = schemeProductBifurcationRepo.findBySchemeId(scheme.getSchemeId());
        return schemeProductBifurcations == null ? scheme : schemeProductBifurcations;
    }

    @Override
    public CkSchemeProductBifurcations save(CkSchemeProductBifurcations scheme) {
        CkSchemeProductBifurcations refreshedScheme = refresh(scheme);
        return super.save(refreshedScheme);
    }

}
