package com.salescode.dim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dim.utils.EventListenerDTO;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;

public class EventListenerDTOSerializer implements KafkaRecordSerializationSchema<EventListenerDTO> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final String topic;

    public EventListenerDTOSerializer(String topic) {
        this.topic = topic;
    }

    @Override
    public ProducerRecord<byte[], byte[]> serialize(EventListenerDTO element, KafkaSinkContext context, Long timestamp) {
        try {
            byte[] key = element.getRequestId().getBytes(); // Key as requestId
            byte[] value = objectMapper.writeValueAsBytes(element); // Convert DTO to JSON bytes

            long eventTimestamp = System.currentTimeMillis(); // Use processing time

            return new ProducerRecord<>(topic, null, eventTimestamp, key, value);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing EventListenerDTO", e);
        }
    }
}
