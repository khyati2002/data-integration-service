package com.applicate.services.channelkart.transformers.impl;

import com.applicate.services.channelkart.models.enums.TargetType;
import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService.TransformationException;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.Optional;

public class GenericTargetTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static Logger logger = LoggerFactory.getLogger(GenericTargetTransformer.class);

    private static final String USER_VALUE = "userValue";
    private static final String OUTLET_VALUE = "outletValue";
    private static final String PRODUCT_VALUE = "productValue";
    private static final String TARGET_ID = "targetId";
    private static final String TARGET_ENTITY = "targetEntity";
    private static final String EXTENDED_ATTRIBUTES = "extendedAttributes";
    private static final String TARGET = "target";
    private static final String TARGET_PERCENTAGES = "target_percentages";
    private static final String USER_TYPE = "userType";
    private static final String OUTLET_TYPE = "outletType";
    private static final String PRODUCT_TYPE = "productType";
    private static final String UNIT = "unit";
    private static final String TARGET_NAME = "targetName";
    private static final String TARGET_TYPE = "targetType";
    private static final String TARGET_TABLE = "targetTable";
    private static final String TARGET_CONDITION = "targetCondition";
    private static final String TARGET_CONDITION_UNIT = "targetConditionUnit";

    private static final ObjectMapper objectMapper = JSONUtils.getObjectMapper();

    @Override
    public Map<String,Object> transform(Map<String, Object> targetMapObj) {

        com.salescode.dim.jooq.generated.tables.pojos.TransformerInfo pojoTransformerInfo = this.getTransformerInfo();
        org.jooq.JSON jooqCode = pojoTransformerInfo.getCode();

        if (NullUtils.isNotNull(jooqCode) && jooqCode.data() != null && NullUtils.isNotNull(targetMapObj)) {
            try {
                String codeNodeString = jooqCode.data();
                ArrayNode shadedCodeNode = (ArrayNode) objectMapper.readTree(codeNodeString);
                Object spec = JsonUtils.jsonToObject(String.valueOf(shadedCodeNode));

                Chainr chainr = Chainr.fromSpec(spec);
                Object transformedOutput = chainr.transform(targetMapObj);
                String prettyJsonString = JsonUtils.toPrettyJsonString(transformedOutput);

                return getTransformValue(targetMapObj, objectMapper.readValue(prettyJsonString, new TypeReference<Map<String, Object>>() {}));
            } catch (Exception ex) {
                throw new TransformationException(ex.getLocalizedMessage());
            }
        } else {
            throw new TransformationException("Either jolt specification/input json found null");
        }
    }

    public Map<String, Object> getTransformValue(Map<String, Object> targetMap, Map<String, Object> result) {

        final MetaDataService metaDataService = (MetaDataService) ServiceLocator.lookup(Metadata.class);
        JsonNode targetConfig = metaDataService.fetchByValue("TARGET_ACHIEVED", "calculation", true).getDomainValues().get(0);

        try {
            if (targetMap.containsKey(USER_VALUE))
                getUserTransform(targetMap, result, targetConfig);
            if (targetMap.containsKey(OUTLET_VALUE))
                getOutletTransform(targetMap, result, targetConfig);
            if (targetMap.containsKey(PRODUCT_VALUE))
                getProductTransform(targetMap, result, targetConfig);

            if (result.containsKey(TARGET_ENTITY)) {
                result.put(TARGET_ID, targetMap.get(TARGET_ID));
                fillCommonAttribues(targetMap, result, targetConfig);
                calculateTargetValue(result);
                result.remove(TARGET_ENTITY);
            } else
                throw new IllegalArgumentException(
                        "<<< Input data incorrect,plz check sheet (userValue,outletvalue,productValue) >>");
        } catch (Exception e) {
            throw new TransformationException(e.getLocalizedMessage());
        }

        return result;
    }

    private void calculateTargetValue(Map<String, Object> result) {
        ObjectNode extendedAttributes = (ObjectNode) result.get(EXTENDED_ATTRIBUTES);
        if (extendedAttributes != null && !extendedAttributes.isNull()) {

            Map<String, Object> extendedAttribute = objectMapper.convertValue(extendedAttributes, new TypeReference<Map<String, Object>>() {});

            String tPercentage = extendedAttribute.get(TARGET_PERCENTAGES).toString();
            String trgt = result.get(TARGET).toString();
            float targetPercentage = Float.parseFloat(tPercentage);
            float target = Float.parseFloat(trgt);
            target = (target * targetPercentage) / 100;
            result.put(TARGET, String.valueOf(target));
        } else
            throw new TransformationException("Can't calculate the target value as the extended attributes are null");
    }

    public void getUserTransform(Map<String, Object> targetMap, Map<String, Object> result, JsonNode targetConfig) {
        fillAtrributeFromInputData(targetMap, result, USER_TYPE, targetConfig);
        String userValue = targetMap.get(USER_VALUE).toString();
        String[] userValueArr = userValue.split(",");
        ArrayNode userValueArrNod = objectMapper.createArrayNode();
        for (String s : userValueArr) {
            userValueArrNod.add(s);
        }
        result.put(USER_VALUE, userValueArrNod);
        result.put(TARGET_ENTITY, TargetType.USER);
    }

    public void getOutletTransform(Map<String, Object> targetMap, Map<String, Object> result, JsonNode targetConfig) {
        fillAtrributeFromInputData(targetMap, result, OUTLET_TYPE, targetConfig);
        String outletValue = targetMap.get(OUTLET_VALUE).toString();
        String[] outletValueArr = outletValue.split(",");
        ArrayNode outletValueArrNod = objectMapper.createArrayNode();
        for (String s : outletValueArr) {
            outletValueArrNod.add(s);
        }
        result.put(OUTLET_VALUE, outletValueArrNod);
        result.put(TARGET_ENTITY, TargetType.OUTLET);
    }

    public void getProductTransform(Map<String, Object> targetMap, Map<String, Object> result, JsonNode targetConfig) {
        fillAtrributeFromInputData(targetMap, result, PRODUCT_TYPE, targetConfig);
        String productValue = targetMap.get(PRODUCT_VALUE).toString();
        String[] productValueArr = productValue.split(",");
        ArrayNode productValueArrNod = objectMapper.createArrayNode();
        for (String s : productValueArr) {
            productValueArrNod.add(s);
        }
        result.put(PRODUCT_VALUE, productValueArrNod);
        result.put(TARGET_ENTITY, TargetType.PRODUCT);
    }

    public void fillCommonAttribues(Map<String, Object> targetMap, Map<String, Object> result, JsonNode targetConfig) {
        ObjectNode extendedAttributes = objectMapper.createObjectNode();
        fillAtrributeFromInputData(targetMap, result, UNIT, targetConfig);
        fillAtrributeFromInputData(targetMap, result, TARGET_NAME, targetConfig);
        fillAtrributeFromInputData(targetMap, result, TARGET_TYPE, targetConfig);
        fillAtrributeFromInputData(targetMap, result, TARGET_TABLE, targetConfig);
        fillAtrributeFromInputData(targetMap, result, TARGET_CONDITION, targetConfig);
        fillAtrributeFromInputData(targetMap, result, TARGET_CONDITION_UNIT, targetConfig);
        fillExtendedAttributesFromInputData(targetMap, extendedAttributes, result, TARGET_PERCENTAGES, targetConfig);
    }

    public void fillAtrributeFromInputData(Map<String, Object> targetMap, Map<String, Object> result, String key, JsonNode targetConfig) {
        Optional.ofNullable(targetMap.get(key)).ifPresentOrElse(i -> result.put(key, targetMap.get(key)),
                () -> fillAtrributeFromConfig(result, key, targetConfig));
    }

    public void fillAtrributeFromConfig(Map<String, Object> result, String key, JsonNode targetConfig) {
        Optional.ofNullable(targetConfig.get(key)).ifPresentOrElse(i -> result.put(key, targetConfig.get(key)),
                () -> {
                    logger.error(String.format("Mandatory meta data config not found for | Key : %s", key));
                    throw new IllegalArgumentException(
                            "<<< Input data incorrect,plz check sheet OR Mandatory meta data config for | Key : " + key + " >>");
                });
    }

    public void fillExtendedAttributesFromInputData(Map<String, Object> targetMap, ObjectNode extendedAttributes, Map<String, Object> result, String key, JsonNode targetConfig) {
        Optional.ofNullable(targetMap.get(key)).ifPresentOrElse(i -> extendedAttributes.put(key, targetMap.get(key).toString()),
                () -> fillExtendedAttributesFromConfig(extendedAttributes, key, targetConfig));
        result.put(EXTENDED_ATTRIBUTES, extendedAttributes);
    }

    public void fillExtendedAttributesFromConfig(ObjectNode extendedAttributes, String key, JsonNode targetConfig) {
        Optional.ofNullable(targetConfig.get(key)).ifPresentOrElse(i -> extendedAttributes.put(key, targetConfig.get(key).asText()),
                () -> {
                    logger.error(String.format("Mandatory meta data config not found for | Key : %s", key));
                    throw new IllegalArgumentException(
                            "<<< Input data incorrect,plz check sheet OR Mandatory meta data config for | Key : " + key + " >>");
                });
    }
}