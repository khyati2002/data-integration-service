package com.salescode.dim.services;
import com.applicate.services.channelkart.services.AbstractCDMService;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeMustBuyGroup;
import com.salescode.dim.repository.SchemeMustBuyGroupRepo;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_MUST_BUY_GROUP;
import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_PRODUCT_BIFURCATIONS;

public class SchemeMustBuyGroupService extends AbstractCDMService<SchemeMustBuyGroup> {
    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(SchemeMustBuyGroupService.class);
    private static DSLContext dsl;


    /** The repository. */
    private SchemeMustBuyGroupRepo schemeMustBuyGroupRepo;

    public SchemeMustBuyGroupService(DSLContext dsl) {
        super(dsl);
        this.dsl = dsl;
    }

    public List<SchemeMustBuyGroup> findBySchemeId(String schemeId) {
        List<SchemeMustBuyGroup> schemeMustBuyGroupList = schemeMustBuyGroupRepo.findBySchemeId(schemeId);
        if (schemeMustBuyGroupList == null ) {
            return null;
        }
        return schemeMustBuyGroupList;
    }
    public void smbSave(List<SchemeMustBuyGroup> bifurcations) {
        if (bifurcations != null) {
            bifurcations.forEach(smb -> {
                smb.setId(UUID.randomUUID().toString());
                smb.setVersion(0);
                super.addHash(smb);
            });
        }
        dsl.batchInsert(
                bifurcations.stream()
                        .map(smb -> dsl.newRecord(CK_SCHEME_MUST_BUY_GROUP, smb))
                        .collect(Collectors.toList())
        ).execute();
    }
    @Override
    public SchemeMustBuyGroup save(SchemeMustBuyGroup scheme) {
        return null;
    }
}
