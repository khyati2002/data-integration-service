package com.applicate.services.channelkart.transformers.impl;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.salescode.dim.jooq.generated.tables.pojos.TransformerInfo;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class JoltTransformer extends AbstractTransformer<Map<String, Object>, Object> {

    protected Map<String, Chainr> templateCompilationCache = new ConcurrentHashMap<>();

    @Override
    @SneakyThrows
    public Object transform(Map<String, Object> stringObjectMap) {
        TransformerInfo transformerInfo = this.getTransformerInfo();
        String code = transformerInfo.getCode().data();
        final ArrayNode codeNode = JSONUtils.getObjectMapper().readValue(code, ArrayNode.class);
        if (codeNode != null && stringObjectMap != null) {
            try {
                Chainr chainr = templateCompilationCache.computeIfAbsent(transformerInfo.getId(), (r) -> Chainr.fromSpec(JsonUtils.jsonToObject(String.valueOf(codeNode))));
                Object transformedOutput = chainr.transform(stringObjectMap);
                String prettyJsonString = JsonUtils.toJsonString(transformedOutput);
                return JSONUtils.getObjectMapper().readValue(prettyJsonString, new TypeReference<HashMap<String, Object>>() {});
            } catch (Exception ex) {
                log.error("Jolt Transformer Exception", ex);
            }
            return null;
        } else {
            throw new NullPointerException("Either jolt specification/input json found null");
        }
    }
}
