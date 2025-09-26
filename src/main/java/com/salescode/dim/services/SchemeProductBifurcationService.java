package com.salescode.dim.services;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.AbstractCDMService;
import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.utils.IDGenerator;
import com.salescode.dim.jooq.generated.Tables;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeProductBifurcations;
import com.salescode.dim.jooq.generated.tables.records.CkSchemeProductBifurcationsRecord;
import com.salescode.dim.repository.SchemeProductBifurcationRepo;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_PRODUCT_BIFURCATIONS;

public class SchemeProductBifurcationService extends AbstractCDMService<SchemeProductBifurcations> {

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(SchemeProductBifurcationService.class);
//    private static DSLContext dsl;

    /**
     * The repository.
     */
    private SchemeProductBifurcationRepo schemeProductBifurcationRepo;
    private static IDGenerator idGenerator = null;
    private static MetaDataService metaDataService;
    private static final String DOMAIN_NAME = "SchemeProductBifurcations";
    private static final String DOMAIN_TYPE = "DynamicUniqueKey";


    public SchemeProductBifurcationService() {
//        this.dsl = getDslContext();
        idGenerator = new IDGenerator();
        metaDataService = new MetaDataService();

    }

    public List<SchemeProductBifurcations> findBySchemeId(String schemeId) {

        List<SchemeProductBifurcations> schemeProductBifurcationsList = schemeProductBifurcationRepo.findBySchemeId(schemeId);
        if (schemeProductBifurcationsList == null) {
            return null;
        }
        return schemeProductBifurcationsList;

    }

    private BiFunction<SchemeProductBifurcations, DSLContext, InsertSetMoreStep<?>> schemeProductBiFunctionMapper = (ros, dslContext) -> {
        return (InsertSetMoreStep<CkSchemeProductBifurcationsRecord>)
                dslContext.insertInto(CK_SCHEME_PRODUCT_BIFURCATIONS)
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.ID, ros.getId())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.ACTIVE_STATUS, ActiveStatus.ACTIVE)
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.ACTIVE_STATUS_REASON, ros.getActiveStatusReason())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.CHANGED, ros.getChanged())
                        .set(CK_SCHEME_PRODUCT_BIFURCATIONS.CREATED_BY, "flink job")
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.CREATION_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(CK_SCHEME_PRODUCT_BIFURCATIONS.EXTENDED_ATTRIBUTES, ros.getExtendedAttributes())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.HASH, ros.getHash())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.LAST_MODIFIED_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.LOB, ros.getLob())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.MODIFIED_BY, ros.getModifiedBy())
                        .set(CK_SCHEME_PRODUCT_BIFURCATIONS.EAN_NUMBER, ros.getEanNumber())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.SOURCE, ros.getSource())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.VERSION, ros.getVersion())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.BATCH_CODE, ros.getBatchCode())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.BRAND, ros.getBrand())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.CATEGORY, ros.getCategory())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.SCHEME_ID, ros.getSchemeId())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.ITEM_CLASS, ros.getItemClass())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.ITEM_ID, ros.getItemId())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.PIECE_SIZE, ros.getPieceSize())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.SUB_CATEGORY, ros.getSubCategory())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.CUSTOM_GROUP_CODE, ros.getCustomGroupCode())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.CTG, ros.getCtg())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.FLAVOUR, ros.getFlavour())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.MARKET_SKU, ros.getMarketSku())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.PIECE_SIZE_DESC, ros.getPieceSizeDesc())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.PURCHASE_UNIT, ros.getPurchaseUnit())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.SIZE, ros.getSize())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.SUB_CATEGORY_CODE, ros.getSubCategoryCode())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.QUALIFIER, ros.getQualifier_())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.PRODUCT, ros.getProduct())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.ITEM_TYPE, ros.getItemType())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.ARTICLE_CODE, ros.getArticleCode())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.SKU_CODE, ros.getSkuCode())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.M_CODE, ros.getMCode())
                        .set(CK_SCHEME_PRODUCT_BIFURCATIONS.CHANGED, false)
                        .onConflict(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.ID)
                        .doUpdate()
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.ACTIVE_STATUS, ActiveStatus.ACTIVE)
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.ACTIVE_STATUS_REASON, ros.getActiveStatusReason())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.CHANGED, ros.getChanged())
                        .set(CK_SCHEME_PRODUCT_BIFURCATIONS.CREATED_BY, "flink job")
                        .set(CK_SCHEME_PRODUCT_BIFURCATIONS.EXTENDED_ATTRIBUTES, ros.getExtendedAttributes())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.HASH, ros.getHash())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.LAST_MODIFIED_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.LOB, ros.getLob())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.MODIFIED_BY, ros.getModifiedBy())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.SOURCE, ros.getSource())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.VERSION, ros.getVersion())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.BATCH_CODE, ros.getBatchCode())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.BRAND, ros.getBrand())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.CATEGORY, ros.getCategory())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.SCHEME_ID, ros.getSchemeId())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.ITEM_CLASS, ros.getItemClass())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.ITEM_ID, ros.getItemId())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.PIECE_SIZE, ros.getPieceSize())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.SUB_CATEGORY, ros.getSubCategory())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.CUSTOM_GROUP_CODE, ros.getCustomGroupCode())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.CTG, ros.getCtg())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.FLAVOUR, ros.getFlavour())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.MARKET_SKU, ros.getMarketSku())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.PIECE_SIZE_DESC, ros.getPieceSizeDesc())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.PURCHASE_UNIT, ros.getPurchaseUnit())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.SIZE, ros.getSize())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.SUB_CATEGORY_CODE, ros.getSubCategoryCode())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.QUALIFIER, ros.getQualifier_())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.PRODUCT, ros.getProduct())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.ITEM_TYPE, ros.getItemType())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.ARTICLE_CODE, ros.getArticleCode())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.SKU_CODE, ros.getSkuCode())
                        .set(Tables.CK_SCHEME_PRODUCT_BIFURCATIONS.M_CODE, ros.getMCode())
                        .set(CK_SCHEME_PRODUCT_BIFURCATIONS.EAN_NUMBER, ros.getEanNumber())
                        .set(CK_SCHEME_PRODUCT_BIFURCATIONS.CHANGED, true);
    };

    public void spbSave(List<SchemeProductBifurcations> bifurcations, DSLContext transDSL ) {
        logger.info("starting saving SchemeProductBifurcations...");
        long currentTime = System.currentTimeMillis();
        JsonNode metadata = metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE).getDomainValues();

        for (SchemeProductBifurcations spb : bifurcations) {
            spb.setId(idGenerator.getIdWithMetaData(spb, metadata));
        }
        transDSL.batch(
                    bifurcations.stream()
                            .map(spb -> schemeProductBiFunctionMapper.apply(spb, transDSL))
                            .collect(Collectors.toList())
            ).execute();

        logger.info("Saved scheme product bifurcations");
        logger.info("Time taken for schemeProductBifurcations : {}", System.currentTimeMillis() - currentTime);

    }
    public List<SchemeProductBifurcations> updateWithIds(List<SchemeProductBifurcations> bifurcations){
        JsonNode metadata = metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE).getDomainValues();

        for (SchemeProductBifurcations spb : bifurcations) {
            spb.setId(idGenerator.getIdWithMetaData(spb, metadata));
        }
        return bifurcations;
    }

    @Override
    public SchemeProductBifurcations save(SchemeProductBifurcations scheme) {
        return null;
    }
}
