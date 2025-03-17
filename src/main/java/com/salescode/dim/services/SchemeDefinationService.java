package com.salescode.dim.services;
import com.applicate.services.channelkart.services.AbstractCDMService;
import com.salescode.dim.jooq.generated.tables.*;
import com.salescode.dim.jooq.impl.SchemeDefination;
import com.salescode.dim.repository.SchemeDefinationRepo;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;
import static com.salescode.dim.jooq.generated.Tables.*;

public class SchemeDefinationService extends AbstractCDMService<SchemeDefination> {
    private DSLContext dsl = null;

    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(CkSchemeDefination.class);

    /** The repository. */
    private static SchemeDefinationRepo schemeDefinationRepo;
    private SchemeProductBifurcationService schemeProductBifurcationService = new SchemeProductBifurcationService(dsl);
    private SchemeLocationBifurcationService schemeLocationBifurcationService = new SchemeLocationBifurcationService(dsl);
    private SchemeOutletBifurcationService schemeOutletBifurcationService = new SchemeOutletBifurcationService(dsl);
    private SchemeCalculationService schemeCalculationService = new SchemeCalculationService(dsl);
    
    public SchemeDefinationService(DSLContext dsl, SchemeDefinationRepo schemeDefinationRepo) {
        super(dsl);
        this.dsl = dsl;
        this.schemeDefinationRepo = schemeDefinationRepo;
    }
    public SchemeDefinationService(DSLContext dsl ) {
        super(dsl);
        this.dsl = dsl;
    }

    public SchemeDefination findBySchemeId(String schemeId) {

        SchemeDefination schemeDefination = schemeDefinationRepo.findBySchemeId(schemeId);
        if (schemeDefination == null ) {
            return null;
        }
        return schemeDefination;
    }
    public void sdSave(SchemeDefination schemeDefination){


        if (schemeDefination != null) {
            schemeDefination.setId(UUID.randomUUID().toString());
            schemeDefination.setVersion(0);
            super.addHash(schemeDefination);

        }
        dsl.batchInsert(
                dsl.newRecord(CK_SCHEME_DEFINATION, schemeDefination)
        ).execute();
    }



    @Override
    public SchemeDefination save(SchemeDefination scheme) {

        schemeProductBifurcationService.spbSave(scheme.getSchemeProductBifurcationsList());
        schemeLocationBifurcationService.slbSave(scheme.getSchemeLocationBifurcationsList());
        schemeOutletBifurcationService.sobSave(scheme.getSchemeOutletBifurcationsList());
        schemeCalculationService.scSave(scheme.getSchemeCalculation().get(0));

        sdSave(scheme);
        return scheme;
    }
}
