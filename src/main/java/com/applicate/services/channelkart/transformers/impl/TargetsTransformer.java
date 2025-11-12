package com.applicate.services.channelkart.transformers.impl;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.generated.tables.pojos.TransformerInfo;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TargetsTransformer extends AbstractTransformer<Map<String,Object>,Map<String,Object>> {

    private static Logger logger = LoggerFactory.getLogger(TargetsTransformer.class);
    private static final ObjectMapper objectMapper = JSONUtils.getObjectMapper();

    private static final String USER_VALUE = "userValue";
    private static final String OUTLET_VALUE = "outletValue";
    private static final String PRODUCT_VALUE = "productValue";


    @Override
    public Map<String,Object> transform(Map<String, Object> s) {
        TransformerInfo transformerInfo= this.getTransformerInfo();

        org.jooq.JSON jooqCode = transformerInfo.getCode();

        if(NullUtils.isNotNull(jooqCode) && jooqCode.data() != null && NullUtils.isNotNull(s)) {
            try {
                String codeNodeString = jooqCode.data();
                ArrayNode shadedCodeNode = (ArrayNode) objectMapper.readTree(codeNodeString);
                Object spec = JsonUtils.jsonToObject(String.valueOf(shadedCodeNode));

                Chainr chainr = Chainr.fromSpec(spec);
                Object transformedOutput = chainr.transform(s);

                String prettyJsonString = JsonUtils.toPrettyJsonString(transformedOutput);
                Map<String,Object> result = objectMapper.readValue(prettyJsonString, new TypeReference<Map<String,Object>>(){});

                processCsvToNode(s, result, USER_VALUE);
                processCsvToNode(s, result, OUTLET_VALUE);
                processCsvToNode(s, result, PRODUCT_VALUE);

                return result;
            }
            catch(Exception ex) {
                logger.error("Jolt Transformer Exception",ex);
            }
        }
        else {
            throw new NullPointerException("Either jolt specification/input json found null");
        }
        return Collections.emptyMap();
    }

    /**
     * Checks if a key exists in the input map, splits its comma-separated value,
     * and puts it into the result map as a Flink/shaded ArrayNode.
     *
     * @param inputMap  The original input map
     * @param resultMap The map being transformed
     * @param key       The key to process (e.g., "userValue")
     */
    private void processCsvToNode(Map<String, Object> inputMap, Map<String, Object> resultMap, String key) {
        if(inputMap.containsKey(key)) {
            String valueString = inputMap.get(key).toString();
            String[] valueArr = valueString.split(",");
            ArrayNode valueArrNode = objectMapper.createArrayNode();
            for (String value : valueArr) {
                valueArrNode.add(value);
            }
            resultMap.put(key, valueArrNode);
        }
    }
}