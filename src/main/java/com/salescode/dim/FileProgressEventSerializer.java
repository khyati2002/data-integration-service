package com.salescode.dim;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.utils.EventListenerDTO;
import com.salescode.dis.insights.dto.event.FileProgressEvent;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class FileProgressEventSerializer implements Serializer<FileProgressEvent> {

    private static final ObjectMapper objectMapper = JSONUtils.getObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // No special configuration needed
    }

    @Override
    public byte[] serialize(String topic, FileProgressEvent data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing EventListenerDTO", e);
        }
    }

    @Override
    public void close() {
        // No resources to close
    }
}
