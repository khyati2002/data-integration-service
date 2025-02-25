package com.salescode.dim.jooq;

import com.applicate.services.channelkart.utils.JSONUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.Converter;
import org.jooq.JSON;

public class JsonNodeConverter implements Converter<JSON, JsonNode> {
    private static final ObjectMapper objectMapper = JSONUtils.getObjectMapper();
    private static final long serialVersionUID = 3472385782530317870L;

    @Override
    public JsonNode from(JSON databaseObject) {
        try {
            return databaseObject == null ? null : objectMapper.readTree(databaseObject.data());
        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON", e);
        }
    }

    @Override
    public JSON to(JsonNode userObject) {
        try {
            return userObject == null ? null : JSON.json(objectMapper.writeValueAsString(userObject));
        } catch (Exception e) {
            throw new RuntimeException("Error serializing JSON", e);
        }
    }

    @Override
    public Class<JSON> fromType() {
        return JSON.class;
    }

    @Override
    public Class<JsonNode> toType() {
        return JsonNode.class;
    }
}