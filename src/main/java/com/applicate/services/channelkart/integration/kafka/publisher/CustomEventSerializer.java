package com.applicate.services.channelkart.integration.kafka.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.applicate.services.channelkart.dto.StreamingEventData;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CustomEventSerializer implements Serializer<StreamingEventData> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomEventSerializer.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }

    @Override
    public byte[] serialize(String s, StreamingEventData streamingEventData) {
        byte[] retVal = null;
        try {
            retVal = mapper.writeValueAsString(streamingEventData).getBytes();
        } catch (Exception exception) {
            LOGGER.error("Error in serializing object" + exception.getMessage());
        }
        return retVal;
    }

    @Override
    public void close() {

    }
}
