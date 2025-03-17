package com.salescode.dim;

import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.formats.json.JsonSerializationSchema;

import java.util.Properties;

public class FlinkJobSink {

    static <T extends KeyedKafkaSerialization> KafkaSink<T> createKafkaSink(Properties outputProperties) {
        ConfigValidator.validate(outputProperties, "lob", "topic", "bootstrap.servers");

        KafkaRecordSerializationSchema<T> recordSerializationSchema = KafkaRecordSerializationSchema.<T>builder()
                                                                                                    .setTopic(outputProperties.getProperty("lob") + outputProperties.getProperty("topic"))
                                                                                                    // Use a field as kafka record key
                                                                                                    // Define no keySerializationSchema to publish kafka records with no key
                                                                                                    .setKeySerializationSchema(srd -> srd.getRequestId()
                                                                                                                                         .getBytes())
                                                                                                    // Serialize the Kafka record value (payload) as JSON
                                                                                                    .setValueSerializationSchema(new JsonSerializationSchema<>())
                                                                                                    .build();


        return KafkaSink.<T>builder()
                        .setBootstrapServers(outputProperties.getProperty("bootstrap.servers"))
                        .setKafkaProducerConfig(outputProperties)
                        .setRecordSerializer(recordSerializationSchema)
//                        .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                        .build();
    }
}