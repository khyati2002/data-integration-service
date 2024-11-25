package com.salescode.dataintegration.etl.transformer.impl;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.channelkart.utils.NullUtils;
import com.salescode.dataintegration.etl.transformer.AbstractTransformer;
import com.salescode.jooq.generated.tables.pojos.CkTransformerInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class JoltTransformer extends AbstractTransformer<Map<String, Object>, Object> {

    @Override
    @SneakyThrows
    public Object transform(Map<String, Object> stringObjectMap) {
        CkTransformerInfo transformerInfo = this.getTransformerInfo();
        String code = transformerInfo.getCode().data();
        final ArrayNode codeNode = JSONUtils.getObjectMapper().readValue(code, ArrayNode.class);
        if (NullUtils.isNotNull(codeNode) && NullUtils.isNotNull(stringObjectMap)) {
            try {
                Object spec = JsonUtils.jsonToObject(String.valueOf(codeNode));
                Chainr chainr = Chainr.fromSpec(spec);
                Object transformedOutput = chainr.transform(stringObjectMap);
                return JSONUtils.getObjectMapper().readValue(JsonUtils.toPrettyJsonString(transformedOutput), new TypeReference<HashMap<String, Object>>() {
                });
            } catch (Exception ex) {
                log.error("Jolt Transformer Exception", ex);
                return null;
            }
        } else {
            throw new NullPointerException("Either jolt specification/input json found null");
        }
    }
}
