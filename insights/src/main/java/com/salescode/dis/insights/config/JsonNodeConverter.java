package com.salescode.dis.insights.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
            if (dbData != null) {
                JsonNode node = mapper.readTree(dbData);
                if (node.isTextual()) {
                    String textValue = node.asText();
                    JsonNode parsedNode = mapper.readTree(textValue);
                    if (parsedNode.isObject()) {
                        return parsedNode;
                    } else if (parsedNode.isArray()) {
                        return parsedNode;
                    } else {
                        return mapper.createObjectNode();
                    }
                }
                return node;
            }
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert String to JsonNode", e);
        }
    }
}