package com.salescode.dim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dim.StreamingRawData;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class StreamingRawDataSerializer implements Serializer<StreamingRawData> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // You can configure ObjectMapper here if needed, e.g.,
        // objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public byte[] serialize(String topic, StreamingRawData data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Error serializing StreamingRawData to JSON", e);
        }
    }

    @Override
    public void close() {
        // No resources to close for ObjectMapper
    }
}