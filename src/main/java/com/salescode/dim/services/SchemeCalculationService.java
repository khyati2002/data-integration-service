package com.salescode.dim.services;
import com.applicate.services.channelkart.services.AbstractCDMService;
import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.jooq.generated.tables.CkSchemeCalculation;
import com.salescode.dim.jooq.generated.tables.CkSchemeMustBuyGroup;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeCalculation;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeMustBuyGroup;
import com.salescode.dim.repository.SchemeCalculationRepo;
import com.salescode.dim.repository.SchemeMustBuyGroupRepo;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.*;

public class SchemeCalculationService extends AbstractCDMService<SchemeCalculation> {

    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(SchemeMustBuyGroupService.class);

    /** The repository. */
    private SchemeCalculationRepo schemeCalculationRepo;
    private SchemeMustBuyGroupRepo schemeMustBuyGroupRepo;
    private final DSLContext dsl;
    private SchemeMustBuyGroupService schemeMustBuyGroupService;
    private SchemeFreeProductInfoService schemeFreeProductInfoService;

    public SchemeCalculationService(DSLContext dsl) {
        super(dsl);
        this.schemeCalculationRepo = schemeCalculationRepo;
        this.dsl = dsl;
        schemeMustBuyGroupService = new SchemeMustBuyGroupService(dsl);
        schemeFreeProductInfoService = new SchemeFreeProductInfoService(dsl);
    }

    public SchemeCalculation findBySchemeId(String schemeId) {

        SchemeCalculation schemeCalculation = schemeCalculationRepo.findBySchemeId(schemeId);
        if (schemeCalculation == null ) {
            return null;
        }
        return schemeCalculation;
    }

    public void scSave(SchemeCalculation schemeCalculation) {
        schemeMustBuyGroupService.smbSave(schemeMustBuyGroupService.findBySchemeId(schemeCalculation.getSchemeId()));
        schemeFreeProductInfoService.sfpSave(schemeFreeProductInfoService.findBySchemeId(schemeCalculation.getSchemeId()));

        if (schemeCalculation != null) {
            schemeCalculation.setId(UUID.randomUUID().toString());
            schemeCalculation.setVersion(0);
            super.addHash(schemeCalculation);
        }
        JsonNode previousSlabArray = schemeCalculation.getSlabInfo();
        Map<String, JsonNode> slabMap = new HashMap<>();
        if(NullUtils.isNotNull(previousSlabArray)){
            previousSlabArray.forEach(slab -> slabMap.put(slab.get("startRange").asText(), slab));
        }
        ArrayNode newSlabArray = new ObjectMapper().createArrayNode();

        JsonNode slabNode = schemeCalculation.getSlabInfo();
        if(NullUtils.isNotNull(slabNode)){
            slabNode.forEach(slab -> slabMap.put(slab.get("startRange").asText(), slab));
        }

        slabMap.values().forEach(newSlabArray::add);

        if(schemeCalculation.getUsabilityPeriod()==null && schemeCalculation.getUsabilityPeriodLimit()==null){
            schemeCalculation.setUsabilityPeriod(null);
            schemeCalculation.setUsabilityPeriodLimit(null);
        }
        if(schemeCalculation.getOutletLimitOnOrder()==null){
            schemeCalculation.setOutletLimitOnOrder(null);
        }

        dsl.batchInsert(
                dsl.newRecord(CK_SCHEME_CALCULATION, schemeCalculation)
        ).execute();
    }

    @Override
    public SchemeCalculation save(SchemeCalculation scheme) {
//        SchemeCalculation refreshedScheme = refresh(scheme);
        List<SchemeMustBuyGroup> schemeMustBuyGroupList =
                dsl.selectFrom(CK_SCHEME_MUST_BUY_GROUP)
                        .where(CK_SCHEME_MUST_BUY_GROUP.SCHEME_ID.eq(scheme.getSchemeId()))
                        .fetchInto(SchemeMustBuyGroup.class);

        if (!schemeMustBuyGroupList.isEmpty()) {
            schemeMustBuyGroupService.batchSave(schemeMustBuyGroupList);
        }
        return save(scheme);
    }
}
