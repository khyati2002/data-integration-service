package com.salescode.dim;

import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;

import java.util.Properties;

public class FlinkJobSink {

    static <T> KafkaSink<T> createKafkaSink(Properties outputProperties, KafkaRecordSerializationSchema<T> recordSerializationSchema) {
        // Validate required properties
        ConfigValidator.validate(outputProperties, "bootstrap.servers");

        return KafkaSink.<T>builder()
                        .setBootstrapServers(outputProperties.getProperty("bootstrap.servers"))
                        .setKafkaProducerConfig(outputProperties)
                        .setRecordSerializer(recordSerializationSchema)
                        .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                        .build();
    }
}