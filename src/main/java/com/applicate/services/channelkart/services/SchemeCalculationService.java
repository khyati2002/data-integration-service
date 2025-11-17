package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.jooq.generated.Tables;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeFreeproductinfo;
import com.salescode.dim.jooq.impl.SchemeCalculation;
import com.salescode.dim.jooq.generated.tables.records.CkSchemeCalculationRecord;
import com.applicate.services.channelkart.repository.SchemeCalculationRepo;
import com.applicate.services.channelkart.repository.SchemeCalculationRepoImpl;
import com.applicate.services.channelkart.repository.SchemeFreeProductInfoRepoImpl;
import com.applicate.services.channelkart.repository.SchemeMustBuyGroupRepo;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_CALCULATION;

public class SchemeCalculationService extends AbstractCDMService<SchemeCalculation> {

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(SchemeCalculationService.class);

    /**
     * The repository.
     */
    private final SchemeCalculationRepo schemeCalculationRepo;
    private SchemeMustBuyGroupRepo schemeMustBuyGroupRepo;
    private final DSLContext dsl;
    private SchemeMustBuyGroupService schemeMustBuyGroupService;
    private SchemeFreeProductInfoService schemeFreeProductInfoService;
    private static MetaDataService metaDataService;
    private static final String DOMAIN_NAME = "SchemeCalculation";
    private static final String DOMAIN_TYPE = "DynamicUniqueKey";
    private final SchemeFreeProductInfoRepoImpl schemeFreeProductInfoRepoImpl;

    public SchemeCalculationService() {
        this.dsl = getDslContext();
        this.schemeCalculationRepo = new SchemeCalculationRepoImpl(dsl);
        metaDataService = new MetaDataService();
        this.schemeFreeProductInfoRepoImpl = new SchemeFreeProductInfoRepoImpl(dsl);
        schemeFreeProductInfoService = new SchemeFreeProductInfoService();
//        schemeMustBuyGroupService = new SchemeMustBuyGroupService(dsl);
    }

    private BiFunction<SchemeCalculation, DSLContext, InsertSetMoreStep<?>> schemeCalculationBiFunctionMapper = (ros, dslContext) -> {
        return (InsertSetMoreStep<CkSchemeCalculationRecord>)
                dslContext.insertInto(CK_SCHEME_CALCULATION)
                        .set(Tables.CK_SCHEME_CALCULATION.ID, ros.getId())
                        .set(Tables.CK_SCHEME_CALCULATION.ACTIVE_STATUS, ActiveStatus.ACTIVE)
                        .set(Tables.CK_SCHEME_CALCULATION.ACTIVE_STATUS_REASON, ros.getActiveStatusReason())
                        .set(Tables.CK_SCHEME_CALCULATION.CHANGED, ros.getChanged())
                        .set(CK_SCHEME_CALCULATION.CREATED_BY, "flink job")
                        .set(Tables.CK_SCHEME_CALCULATION.CREATION_TIME, ros.getCreationTime())
                        .set(CK_SCHEME_CALCULATION.EXTENDED_ATTRIBUTES, ros.getExtendedAttributes())
                        .set(Tables.CK_SCHEME_CALCULATION.HASH, ros.getHash())
                        .set(Tables.CK_SCHEME_CALCULATION.LAST_MODIFIED_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(Tables.CK_SCHEME_CALCULATION.LOB, ros.getLob())
                        .set(Tables.CK_SCHEME_CALCULATION.MODIFIED_BY, ros.getModifiedBy())
                        .set(Tables.CK_SCHEME_CALCULATION.SOURCE, ros.getSource())
                        .set(Tables.CK_SCHEME_CALCULATION.VERSION, ros.getVersion())
                        .set(Tables.CK_SCHEME_CALCULATION.BENEFIT_LIMIT, ros.getBenefitLimit())
                        .set(Tables.CK_SCHEME_CALCULATION.CRITERIA, ros.getCriteria())
                        .set(Tables.CK_SCHEME_CALCULATION.CUSTOM_GROUP_CODE, ros.getCustomGroupCode())
                        .set(Tables.CK_SCHEME_CALCULATION.SCHEME_ID, ros.getSchemeId())
                        .set(Tables.CK_SCHEME_CALCULATION.ITEM_EACH, ros.getItemEach())
                        .set(Tables.CK_SCHEME_CALCULATION.LIMIT_ON_ORDER, ros.getLimitOnOrder())
                        .set(Tables.CK_SCHEME_CALCULATION.MAX_DISCOUNT, ros.getMaxDiscount())
                        .set(Tables.CK_SCHEME_CALCULATION.MAX_TERM, ros.getMaxTerm())
                        .set(Tables.CK_SCHEME_CALCULATION.MINIMUM_AMOUNT, ros.getMinimumAmount())
                        .set(Tables.CK_SCHEME_CALCULATION.MUST_BUY_GROUP_ID, ros.getMustBuyGroupId())
                        .set(Tables.CK_SCHEME_CALCULATION.RANGE_LEVEL_UNIT, ros.getRangeLevelUnit())
                        .set(Tables.CK_SCHEME_CALCULATION.REPEAT_FOR_EVERY, 0.0)
                        .set(Tables.CK_SCHEME_CALCULATION.SCHEME_DISCOUNTED_PRODUCT_PRICE, ros.getSchemeDiscountedProductPrice())
                        .set(Tables.CK_SCHEME_CALCULATION.SCHEME_DISCOUNTED_PRODUCTCODE, ros.getSchemeDiscountedProductcode())
                        .set(Tables.CK_SCHEME_CALCULATION.SCHEME_DISCOUNTED_PRODUCTCODEUOM, ros.getSchemeDiscountedProductcodeuom())
                        .set(Tables.CK_SCHEME_CALCULATION.SCHEME_DISCOUNTED_PRODUCTCODE_VALUE, ros.getSchemeDiscountedProductcodeValue())
                        .set(Tables.CK_SCHEME_CALCULATION.SCHEME_TYPE, ros.getSchemeType())
                        .set(Tables.CK_SCHEME_CALCULATION.SLAB_INFO, ros.getSlabInfo())
                        .set(Tables.CK_SCHEME_CALCULATION.USAGE_LIMIT, ros.getUsageLimit())
                        .set(Tables.CK_SCHEME_CALCULATION.CUSTOM_FIELD, ros.getCustomField())
                        .set(Tables.CK_SCHEME_CALCULATION.OUTLET_EXCLUSION_KEY, ros.getOutletExclusionKey())
                        .set(Tables.CK_SCHEME_CALCULATION.OUTLET_EXCLUSION_VALUES, ros.getOutletExclusionValues())
                        .set(Tables.CK_SCHEME_CALCULATION.FREE_PRODUCT_INFO_ID, ros.getFreeProductInfoId())
                        .set(Tables.CK_SCHEME_CALCULATION.NTH_ORDER_NUMBER, ros.getNthOrderNumber())
                        .set(Tables.CK_SCHEME_CALCULATION.NTH_ORDER_PERIOD, ros.getNthOrderPeriod())
                        .set(Tables.CK_SCHEME_CALCULATION.USABILITY_PERIOD, ros.getUsabilityPeriod())
                        .set(Tables.CK_SCHEME_CALCULATION.USABILITY_PERIOD_LIMIT, ros.getUsabilityPeriodLimit())
                        .set(Tables.CK_SCHEME_CALCULATION.OUTLET_LIMIT_ON_ORDER, ros.getOutletLimitOnOrder())
                        .set(Tables.CK_SCHEME_CALCULATION.QUOTA_CODE, ros.getQuotaCode())
                        .set(Tables.CK_SCHEME_CALCULATION.CHANGED, false)
                        .set(Tables.CK_SCHEME_CALCULATION.BENEFIT_LIMIT, 0.0)
                        .set(Tables.CK_SCHEME_CALCULATION.LIMIT_ON_ORDER, 0)
                        .onConflict(Tables.CK_SCHEME_CALCULATION.ID)
                        .doUpdate()
                        .set(Tables.CK_SCHEME_CALCULATION.ACTIVE_STATUS, ActiveStatus.ACTIVE)
                        .set(Tables.CK_SCHEME_CALCULATION.ACTIVE_STATUS_REASON, ros.getActiveStatusReason())
                        .set(Tables.CK_SCHEME_CALCULATION.CHANGED, ros.getChanged())
                        .set(CK_SCHEME_CALCULATION.CREATED_BY, "flink job")
                        .set(Tables.CK_SCHEME_CALCULATION.CREATION_TIME, ros.getCreationTime())
                        .set(CK_SCHEME_CALCULATION.EXTENDED_ATTRIBUTES, ros.getExtendedAttributes())
                        .set(Tables.CK_SCHEME_CALCULATION.HASH, ros.getHash())
                        .set(Tables.CK_SCHEME_CALCULATION.LAST_MODIFIED_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(Tables.CK_SCHEME_CALCULATION.LOB, ros.getLob())
                        .set(Tables.CK_SCHEME_CALCULATION.MODIFIED_BY, ros.getModifiedBy())
                        .set(Tables.CK_SCHEME_CALCULATION.SOURCE, ros.getSource())
                        .set(Tables.CK_SCHEME_CALCULATION.VERSION, ros.getVersion())
                        .set(Tables.CK_SCHEME_CALCULATION.BENEFIT_LIMIT, ros.getBenefitLimit())
                        .set(Tables.CK_SCHEME_CALCULATION.CRITERIA, ros.getCriteria())
                        .set(Tables.CK_SCHEME_CALCULATION.CUSTOM_GROUP_CODE, ros.getCustomGroupCode())
                        .set(Tables.CK_SCHEME_CALCULATION.SCHEME_ID, ros.getSchemeId())
                        .set(Tables.CK_SCHEME_CALCULATION.ITEM_EACH, ros.getItemEach())
                        .set(Tables.CK_SCHEME_CALCULATION.LIMIT_ON_ORDER, ros.getLimitOnOrder())
                        .set(Tables.CK_SCHEME_CALCULATION.MAX_DISCOUNT, ros.getMaxDiscount())
                        .set(Tables.CK_SCHEME_CALCULATION.MAX_TERM, ros.getMaxTerm())
                        .set(Tables.CK_SCHEME_CALCULATION.MINIMUM_AMOUNT, ros.getMinimumAmount())
                        .set(Tables.CK_SCHEME_CALCULATION.MUST_BUY_GROUP_ID, ros.getMustBuyGroupId())
                        .set(Tables.CK_SCHEME_CALCULATION.RANGE_LEVEL_UNIT, ros.getRangeLevelUnit())
                        .set(Tables.CK_SCHEME_CALCULATION.REPEAT_FOR_EVERY, 0.0)
                        .set(Tables.CK_SCHEME_CALCULATION.SCHEME_DISCOUNTED_PRODUCT_PRICE, ros.getSchemeDiscountedProductPrice())
                        .set(Tables.CK_SCHEME_CALCULATION.SCHEME_DISCOUNTED_PRODUCTCODE, ros.getSchemeDiscountedProductcode())
                        .set(Tables.CK_SCHEME_CALCULATION.SCHEME_DISCOUNTED_PRODUCTCODEUOM, ros.getSchemeDiscountedProductcodeuom())
                        .set(Tables.CK_SCHEME_CALCULATION.SCHEME_DISCOUNTED_PRODUCTCODE_VALUE, ros.getSchemeDiscountedProductcodeValue())
                        .set(Tables.CK_SCHEME_CALCULATION.SCHEME_TYPE, ros.getSchemeType())
                        .set(Tables.CK_SCHEME_CALCULATION.SLAB_INFO, ros.getSlabInfo())
                        .set(Tables.CK_SCHEME_CALCULATION.USAGE_LIMIT, ros.getUsageLimit())
                        .set(Tables.CK_SCHEME_CALCULATION.CUSTOM_FIELD, ros.getCustomField())
                        .set(Tables.CK_SCHEME_CALCULATION.OUTLET_EXCLUSION_KEY, ros.getOutletExclusionKey())
                        .set(Tables.CK_SCHEME_CALCULATION.OUTLET_EXCLUSION_VALUES, ros.getOutletExclusionValues())
                        .set(Tables.CK_SCHEME_CALCULATION.FREE_PRODUCT_INFO_ID, ros.getFreeProductInfoId())
                        .set(Tables.CK_SCHEME_CALCULATION.NTH_ORDER_NUMBER, ros.getNthOrderNumber())
                        .set(Tables.CK_SCHEME_CALCULATION.NTH_ORDER_PERIOD, ros.getNthOrderPeriod())
                        .set(Tables.CK_SCHEME_CALCULATION.USABILITY_PERIOD, ros.getUsabilityPeriod())
                        .set(Tables.CK_SCHEME_CALCULATION.USABILITY_PERIOD_LIMIT, ros.getUsabilityPeriodLimit())
                        .set(Tables.CK_SCHEME_CALCULATION.OUTLET_LIMIT_ON_ORDER, ros.getOutletLimitOnOrder())
                        .set(Tables.CK_SCHEME_CALCULATION.QUOTA_CODE, ros.getQuotaCode())
                        .set(Tables.CK_SCHEME_CALCULATION.CHANGED, true)
                        .set(Tables.CK_SCHEME_CALCULATION.BENEFIT_LIMIT, 0.0)
                        .set(CK_SCHEME_CALCULATION.LIMIT_ON_ORDER, 0)
                ;
    };

    public SchemeCalculation findBySchemeId(String schemeId) {
        return schemeCalculationRepo.findBySchemeId(schemeId);
    }

    public SchemeCalculation updateWithIds(SchemeCalculation schemeCalculation) {
        JsonNode metadata = metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE).getDomainValues();
        schemeCalculation.setId(new IdGenerator(schemeCalculation.getClass().getSimpleName()).getId(schemeCalculation));
        return schemeCalculation;
    }

    public List<SchemeCalculation> scSave(List<SchemeCalculation> schemeCalculations, DSLContext transDSL) {
        long currentTime = System.currentTimeMillis();

        for (SchemeCalculation schemeCalculation : schemeCalculations) {

//        schemeMustBuyGroupService.smbSave(schemeMustBuyGroupService.findBySchemeId(schemeCalculation.getSchemeId()));
//        schemeFreeProductInfoService.sfpSave(schemeFreeProductInfoService.findBySchemeId(schemeCalculation.getSchemeId()));
            JsonNode metadata = metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE).getDomainValues();
            schemeCalculation.setId(new IdGenerator(schemeCalculation.getClass().getSimpleName()).getId(schemeCalculation));
            CkSchemeCalculationRecord calculationRecord = transDSL.selectFrom(CK_SCHEME_CALCULATION).where(CK_SCHEME_CALCULATION.SCHEME_ID.eq(schemeCalculation.getSchemeId())).fetchOne();
            JsonNode previousSlabArray = JSONUtils.getObjectMapper().createArrayNode();
            if (calculationRecord != null) {
                previousSlabArray = calculationRecord.getValue(CK_SCHEME_CALCULATION.SLAB_INFO);
            }
            Map<String, JsonNode> slabMap = new HashMap<>();
            if (NullUtils.isNotNull(previousSlabArray)) {
                previousSlabArray.forEach(slab -> slabMap.put(slab.get("startRange").asText(), slab));
            }
            ArrayNode newSlabArray = new ObjectMapper().createArrayNode();
            JsonNode slabNode = schemeCalculation.getSlabInfo();
            if (NullUtils.isNotNull(slabNode)) {
                slabNode.forEach(slab -> slabMap.put(slab.get("startRange").asText(), slab));
            }
            slabMap.values().forEach(newSlabArray::add);
            if (schemeCalculation.getUsabilityPeriod() == null && schemeCalculation.getUsabilityPeriodLimit() == null) {
                schemeCalculation.setUsabilityPeriod(null);
                schemeCalculation.setUsabilityPeriodLimit(null);
            }
            if (schemeCalculation.getOutletLimitOnOrder() == null) {
                schemeCalculation.setOutletLimitOnOrder(null);
            }
            schemeCalculation.setSlabInfo(newSlabArray);

            String freeProductInfoId = null;
            if (calculationRecord != null) {
                freeProductInfoId = calculationRecord.getValue(CK_SCHEME_CALCULATION.FREE_PRODUCT_INFO_ID);
            }
            if (NullUtils.isNotNull(freeProductInfoId)) {
                List<SchemeFreeproductinfo> freeproductinfoList = schemeCalculation.getSchemeFreeproductinfoList();
                schemeFreeProductInfoService.sfpSave(freeproductinfoList);
            }
        }
        transDSL.batch(schemeCalculations.stream().map(entity -> schemeCalculationBiFunctionMapper.apply(entity, transDSL)).collect(Collectors.toList())).execute();

        logger.info("Scheme Calculation Saved Successfully");
        logger.info("Time taken for schemeCalculation : {}", System.currentTimeMillis() - currentTime);

        return schemeCalculations;
    }

    @Override
    public SchemeCalculation save(SchemeCalculation scheme) {
        long currentTime = System.currentTimeMillis();
        JsonNode metadata = metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE).getDomainValues();
        scheme.setId(new IdGenerator(scheme.getClass().getSimpleName()).getId(scheme));
        CkSchemeCalculationRecord calculationRecord = dsl.selectFrom(CK_SCHEME_CALCULATION).where(CK_SCHEME_CALCULATION.SCHEME_ID.eq(scheme.getSchemeId())).fetchOne();
        JsonNode previousSlabArray = JSONUtils.getObjectMapper().createArrayNode();
        if (calculationRecord != null) {
            previousSlabArray = calculationRecord.getValue(CK_SCHEME_CALCULATION.SLAB_INFO);
        }
        Map<String, JsonNode> slabMap = new HashMap<>();
        if (NullUtils.isNotNull(previousSlabArray)) {
            previousSlabArray.forEach(slab -> slabMap.put(slab.get("startRange").asText(), slab));
        }
        ArrayNode newSlabArray = new ObjectMapper().createArrayNode();
        JsonNode slabNode = scheme.getSlabInfo();
        if (NullUtils.isNotNull(slabNode)) {
            slabNode.forEach(slab -> slabMap.put(slab.get("startRange").asText(), slab));
        }
        slabMap.values().forEach(newSlabArray::add);
        if (scheme.getUsabilityPeriod() == null && scheme.getUsabilityPeriodLimit() == null) {
            scheme.setUsabilityPeriod(null);
            scheme.setUsabilityPeriodLimit(null);
        }
        if (scheme.getOutletLimitOnOrder() == null) {
            scheme.setOutletLimitOnOrder(null);
        }
        scheme.setSlabInfo(newSlabArray);

        String freeProductInfoId = null;
        if (calculationRecord != null) {
            freeProductInfoId = calculationRecord.getValue(CK_SCHEME_CALCULATION.FREE_PRODUCT_INFO_ID);
        }
        if (NullUtils.isNotNull(freeProductInfoId)) {
            List<SchemeFreeproductinfo> freeproductinfoList = schemeFreeProductInfoRepoImpl.findBySchemeId(scheme.getSchemeId());
            schemeFreeProductInfoService.sfpSave(freeproductinfoList);
        }

        logger.info("Time taken for schemeCalculation : {}", System.currentTimeMillis() - currentTime);

        return scheme;
    }
}
