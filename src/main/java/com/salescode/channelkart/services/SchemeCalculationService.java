package com.salescode.channelkart.services;
import com.salescode.channelkart.repository.SchemeCalculationRepo;
import com.salescode.channelkart.repository.SchemeMustBuyGroupRepo;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.jooq.generated.tables.pojos.CkSchemeCalculation;
import com.salescode.jooq.generated.tables.pojos.CkSchemeMustBuyGroup;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import static com.salescode.jooq.generated.Tables.CK_SCHEME_MUST_BUY_GROUP;

public class SchemeCalculationService extends AbstractCDMService<CkSchemeCalculation> {

    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(SchemeMustBuyGroupService.class);

    /** The repository. */
    private SchemeCalculationRepo schemeCalculationRepo;
    private SchemeMustBuyGroupRepo schemeMustBuyGroupRepo;
    private final DSLContext dsl;
    private SchemeMustBuyGroupService schemeMustBuyGroupService;

    public SchemeCalculationService(SchemeCalculationRepo schemeCalculationRepo, DSLContext dsl) {
        this.schemeCalculationRepo = schemeCalculationRepo;
        this.dsl = dsl;
    }

    public CkSchemeCalculation findBySchemeId(String schemeId) {

        CkSchemeCalculation schemeCalculation = schemeCalculationRepo.findBySchemeId(schemeId);
        if (schemeCalculation == null ) {
            return null;
        }
        return schemeCalculation;
    }
    @Override
    public CkSchemeCalculation refresh(CkSchemeCalculation scheme) {
        List<CkSchemeMustBuyGroup> schemeMustBuyGroupList =
                dsl.selectFrom(CK_SCHEME_MUST_BUY_GROUP)
                        .where(CK_SCHEME_MUST_BUY_GROUP.SCHEME_ID.eq(scheme.getSchemeId()))
                        .fetchInto(CkSchemeMustBuyGroup.class);

        if (!schemeMustBuyGroupList.isEmpty()) {
            List<CkSchemeMustBuyGroup> refreshedData = schemeMustBuyGroupService.refresh(schemeMustBuyGroupList);
            schemeMustBuyGroupService.batchSave(refreshedData);
        }
        CkSchemeCalculation schemeCalculation = schemeCalculationRepo.findBySchemeId(scheme.getSchemeId());
        return schemeCalculation == null ? scheme : schemeCalculation;
    }

    @Override
    public CkSchemeCalculation save(CkSchemeCalculation scheme) {
        CkSchemeCalculation refreshedScheme = refresh(scheme);
        return super.save(refreshedScheme);
    }
}
