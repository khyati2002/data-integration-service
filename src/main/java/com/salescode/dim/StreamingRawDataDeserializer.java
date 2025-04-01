package com.salescode.dim;

import com.applicate.services.channelkart.utils.JSONUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.IOException;

public class StreamingRawDataDeserializer implements KafkaRecordDeserializationSchema<StreamingRawData> {

    private static final long serialVersionUID = 6713663262551507277L;

    private static final ObjectMapper objectMapper = JSONUtils.getObjectMapper();

    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<StreamingRawData> out) throws IOException {
        StreamingRawData streamingRawData = objectMapper.readValue(record.value(), StreamingRawData.class);
        if (streamingRawData.getOffset() == null) {
            streamingRawData.setOffset(record.offset());
        }
        out.collect(streamingRawData);
    }

    @Override
    public TypeInformation<StreamingRawData> getProducedType() {
        return TypeInformation.of(StreamingRawData.class);
    }
}
