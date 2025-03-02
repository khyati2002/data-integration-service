package com.salescode.channelkart.services;
import com.salescode.channelkart.repository.SchemeMustBuyGroupRepo;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.jooq.generated.tables.pojos.CkSchemeMustBuyGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemeMustBuyGroupService extends AbstractCDMService<CkSchemeMustBuyGroup> {
    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(SchemeMustBuyGroupService.class);

    /** The repository. */
    private SchemeMustBuyGroupRepo schemeMustBuyGroupRepo;
    public CkSchemeMustBuyGroup findBySchemeId(String schemeId) {
        CkSchemeMustBuyGroup schemeMustBuyGroup = schemeMustBuyGroupRepo.findBySchemeId(schemeId);
        if (schemeMustBuyGroup == null ) {
            return null;
        }
        return schemeMustBuyGroup;
    }
    @Override
    public CkSchemeMustBuyGroup refresh(CkSchemeMustBuyGroup scheme) {
        CkSchemeMustBuyGroup schemeMustBuyGroup = schemeMustBuyGroupRepo.findBySchemeId(scheme.getSchemeId());
        return schemeMustBuyGroup == null ? scheme : schemeMustBuyGroup;
    }
    @Override
    public CkSchemeMustBuyGroup save(CkSchemeMustBuyGroup scheme) {
        CkSchemeMustBuyGroup refreshedScheme = refresh(scheme);
        return super.save(refreshedScheme);
    }
}
