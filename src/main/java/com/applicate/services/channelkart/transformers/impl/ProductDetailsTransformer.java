package com.applicate.services.channelkart.transformers.impl;

import com.salescode.dim.etl.transformation.service.DataTransformationService.TransformationException;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.generated.tables.pojos.TransformerInfo;

import java.util.Map;

public class ProductDetailsTransformer extends AbstractTransformer<Map<String,Object>,Map<String,Object>>{

    private static final ObjectMapper objectMapper = JSONUtils.getObjectMapper();

    @Override
    public Map<String,Object> transform(Map<String, Object> s) {
        TransformerInfo transformerInfo = this.getTransformerInfo();
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

                if(result != null && !result.isEmpty()) {
                    return result;
                } else {
                    throw new TransformationException("Transformer Error: Transformed output is null or empty");
                }
            }
            catch(Exception ex) {
                throw new TransformationException("Transformer Error: Jolt Transformer Exception . Reason : " + ex.getLocalizedMessage(), ex);
            }
        }
        else {
            throw new TransformationException("Either jolt specification/input json found null");
        }
    }
}