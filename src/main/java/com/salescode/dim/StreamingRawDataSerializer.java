package com.salescode.dim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dim.StreamingRawData;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class StreamingRawDataSerializer implements Serializer<StreamingRawData> {

    private static final Logger LOG = LoggerFactory.getLogger(StreamingRawDataSerializer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // Configure ObjectMapper if needed
    }

    @Override
    public byte[] serialize(String topic, StreamingRawData data) {
        if (data == null) {
            return null;
        }

        try {
            // Convert to Map first, which handles ArrayNode properly
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = objectMapper.convertValue(data, Map.class);

            // ObjectMapper.convertValue properly handles ArrayNode conversion
            byte[] result = objectMapper.writeValueAsBytes(dataMap);
            LOG.debug("Successfully serialized StreamingRawData using convertValue");
            return result;

        } catch (Exception e) {
            LOG.error("Error serializing StreamingRawData to JSON", e);
            throw new SerializationException("Error serializing StreamingRawData to JSON", e);
        }
    }

    @Override
    public void close() {
        // No resources to close
    }
}