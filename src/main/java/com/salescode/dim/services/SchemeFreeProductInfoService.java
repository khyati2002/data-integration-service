package com.salescode.dim.services;

import com.applicate.services.channelkart.services.AbstractCDMService;
import com.applicate.services.channelkart.services.MetaDataService;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeFreeproductinfo;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_FREEPRODUCTINFO;

public class SchemeFreeProductInfoService extends AbstractCDMService<SchemeFreeproductinfo> {

    /** The logger. */
    private static final Logger logger = LoggerFactory.getLogger(SchemeFreeProductInfoService.class);
    private DSLContext dsl = getDslContext();

    private static MetaDataService metaDataService;
    private static final String DOMAIN_NAME = "Schemefreeproductinfo";
    private static final String DOMAIN_TYPE = "DynamicUniqueKey";

    public SchemeFreeProductInfoService() {
        this.dsl = getDslContext();
    }

    public List<SchemeFreeproductinfo> findBySchemeId(String schemeId) {
        List<SchemeFreeproductinfo> schemeFreeproductinfoList = getDslContext().selectFrom(CK_SCHEME_FREEPRODUCTINFO).where(CK_SCHEME_FREEPRODUCTINFO.SCHEME_ID.eq(schemeId)).fetchInto(SchemeFreeproductinfo.class);
        if (schemeFreeproductinfoList == null ) {
            return null;
        }
        return schemeFreeproductinfoList;
    }

    public ObjectNode findExtendedByBatchCode(String batchCode) {
        return  dsl.select(CK_SCHEME_FREEPRODUCTINFO.EXTENDED_ATTRIBUTES)
                .from(CK_SCHEME_FREEPRODUCTINFO)
                .where(CK_SCHEME_FREEPRODUCTINFO.BATCH_CODE.eq(batchCode))
                .fetchOneInto(ObjectNode.class);
    }


    public void setExtendedAttributesByPromoCode(String batchCode, ObjectNode extendedAttributes){
        dsl.update(CK_SCHEME_FREEPRODUCTINFO)
                .set(CK_SCHEME_FREEPRODUCTINFO.EXTENDED_ATTRIBUTES, extendedAttributes)
                .where(CK_SCHEME_FREEPRODUCTINFO.BATCH_CODE.eq(batchCode))
                .execute();
    }
}
