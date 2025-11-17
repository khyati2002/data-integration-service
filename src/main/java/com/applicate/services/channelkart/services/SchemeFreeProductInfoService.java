package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeFreeproductinfo;
import com.applicate.services.channelkart.repository.SchemeFreeProductInfoRepo;
import com.applicate.services.channelkart.repository.SchemeFreeProductInfoRepoImpl;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_FREEPRODUCTINFO;

public class SchemeFreeProductInfoService extends AbstractCDMService<SchemeFreeproductinfo> {

    /** The logger. */
    private static final Logger logger = LoggerFactory.getLogger(SchemeFreeProductInfoService.class);
    private DSLContext dsl = getDslContext();

    private SchemeFreeProductInfoRepo schemeFreeProductInfoRepo;
    private static MetaDataService metaDataService;
    private static final String DOMAIN_NAME = "Schemefreeproductinfo";
    private static final String DOMAIN_TYPE = "DynamicUniqueKey";

    public SchemeFreeProductInfoService() {
        this.dsl = getDslContext();
        schemeFreeProductInfoRepo=new SchemeFreeProductInfoRepoImpl(dsl);
        metaDataService = new MetaDataService();
    }

    public List<SchemeFreeproductinfo> findBySchemeId(String schemeId) {
        List<SchemeFreeproductinfo> schemeFreeproductinfoList = schemeFreeProductInfoRepo.findBySchemeId(schemeId);
        if (schemeFreeproductinfoList == null ) {
            return null;
        }
        return schemeFreeproductinfoList;
    }

    public void sfpSave(List<SchemeFreeproductinfo> bifurcations) {
        JsonNode metadata = metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE).getDomainValues();
        if (bifurcations != null) {
            bifurcations.forEach(sfp -> {
                sfp.setId(new IdGenerator(sfp.getClass().getSimpleName()).getId(sfp));
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
