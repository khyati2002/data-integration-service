// JsonNodeConverter.java
package com.salescode.dis.insights.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dis.insights.utils.JsonUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {

    private static final ObjectMapper mapper = JsonUtils.getObjectMapper();

    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        try {
            return attribute != null ? mapper.writeValueAsString(attribute) : null;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert JsonNode to String", e);
        }
    }

    @Override
    public JsonNode convertToEntityAttribute(String dbData) {
        try {
            return dbData != null ? mapper.readTree(dbData) : null;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert String to JsonNode", e);
        }
    }
}