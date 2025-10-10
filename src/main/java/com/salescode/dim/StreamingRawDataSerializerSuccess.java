package com.salescode.dim;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

public class StreamingRawDataSerializerSuccess implements Serializer<StreamingRawData> {

    private static final Logger LOG = LoggerFactory.getLogger(StreamingRawDataSerializer.class);
    private ObjectMapper objectMapper;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // Use the Flink-shaded ObjectMapper that corresponds to the ArrayNode in StreamingRawData
        this.objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Override
    public byte[] serialize(String topic, StreamingRawData data) {
        if (data == null) {
            return null;
        }

        try {
            JsonNode rootNode = objectMapper.valueToTree(data);
            ObjectNode correctedRootNode = objectMapper.createObjectNode();

            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode fieldValue = field.getValue();

                // Check for the problematic object representation of an array.
                if (fieldValue.isObject() && fieldValue.has("array") && fieldValue.get("array").asBoolean()) {
                    ArrayNode realArray = null;
                    Iterator<JsonNode> children = fieldValue.elements();
                    while (children.hasNext()) {
                        JsonNode child = children.next();
                        if (child.isArray()) {
                            realArray = (ArrayNode) child;
                            break;
                        }
                    }

                    if (realArray != null) {
                        correctedRootNode.set(fieldName, realArray);
                    } else {
                        correctedRootNode.set(fieldName, objectMapper.createArrayNode());
                    }
                } else {
                    correctedRootNode.set(fieldName, fieldValue);
                }
            }

            String jsonString = objectMapper.writeValueAsString(correctedRootNode);
            LOG.debug("Serialized JSON string: {}", jsonString);

            return jsonString.getBytes("UTF-8");

        } catch (Exception e) {
            LOG.error("Error serializing StreamingRawData to JSON", e);
            throw new SerializationException("Error serializing StreamingRawData to JSON", e);
        }
    }

    @Override
    public void close() {
        // No resources to close.
    }
}