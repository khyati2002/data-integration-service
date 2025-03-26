package com.salescode.dim.services;

import com.applicate.services.channelkart.services.AbstractCDMService;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeFreeproductinfo;
import com.salescode.dim.repository.SchemeFreeProductInfoRepo;
import com.salescode.dim.repository.SchemeFreeProductInfoRepoImpl;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_FREEPRODUCTINFO;

public class SchemeFreeProductInfoService extends AbstractCDMService<SchemeFreeproductinfo> {

    /** The logger. */
    private static final Logger logger = LoggerFactory.getLogger(SchemeFreeProductInfoService.class);
    private DSLContext dsl = getDslContext();

    private SchemeFreeProductInfoRepo schemeFreeProductInfoRepo;

    public SchemeFreeProductInfoService() {
        this.dsl = getDslContext();
        schemeFreeProductInfoRepo=new SchemeFreeProductInfoRepoImpl(dsl);
    }

    public List<SchemeFreeproductinfo> findBySchemeId(String schemeId) {
        List<SchemeFreeproductinfo> schemeFreeproductinfoList = schemeFreeProductInfoRepo.findBySchemeId(schemeId);
        if (schemeFreeproductinfoList == null ) {
            return null;
        }
        return schemeFreeproductinfoList;
    }

    public void sfpSave(List<SchemeFreeproductinfo> bifurcations) {
        if (bifurcations != null) {
            bifurcations.forEach(sfp -> {
                sfp.setId(UUID.randomUUID().toString());
                sfp.setVersion(0);
                super.addHash(sfp);
            });
        }
        dsl.batchInsert(
                bifurcations.stream()
                        .map(sfp -> dsl.newRecord(CK_SCHEME_FREEPRODUCTINFO, sfp))
                        .collect(Collectors.toList())
        ).execute();
    }


    @Override
    public SchemeFreeproductinfo save(SchemeFreeproductinfo cdmObject) {
        return null;
    }
}
