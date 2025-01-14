package com.salescode.channelkart.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.channelkart.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;

    public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {

        private static final Logger log = LoggerFactory.getLogger(JsonNodeConverter.class);
        @Override
        public String convertToDatabaseColumn(JsonNode attribute) {
            if (attribute != null) {
                return attribute.toString();
            }
            return null;
        }

        @Override
        public JsonNode convertToEntityAttribute(String jsonValue) {
            if (jsonValue != null) {
                try {
                    return JSONUtils.toJsonNode(jsonValue);
                }catch (Exception e) {
                    log.error("stacktrace", e);
                }
            }
            return null;
        }
}
