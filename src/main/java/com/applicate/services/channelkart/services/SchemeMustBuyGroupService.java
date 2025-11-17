package com.applicate.services.channelkart.services;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeMustBuyGroup;
import com.applicate.services.channelkart.repository.SchemeMustBuyGroupRepo;
import com.applicate.services.channelkart.repository.SchemeMustBuyGroupRepoImpl;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_MUST_BUY_GROUP;

public class SchemeMustBuyGroupService extends AbstractCDMService<SchemeMustBuyGroup> {
    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(SchemeMustBuyGroupService.class);
    private static DSLContext dsl;


    /** The repository. */
    private SchemeMustBuyGroupRepo schemeMustBuyGroupRepo;

    public SchemeMustBuyGroupService() {
        this.dsl = getDslContext();
        this.schemeMustBuyGroupRepo = new SchemeMustBuyGroupRepoImpl(dsl);
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
                smb.setId(new IdGenerator(smb.getClass().getSimpleName()).getId(smb));
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
