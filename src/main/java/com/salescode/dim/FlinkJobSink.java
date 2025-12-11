package com.salescode.dim;

import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.TopicSelector;
import org.apache.flink.formats.json.JsonSerializationSchema;

import java.util.Properties;

public class FlinkJobSink {

    static <T> KafkaSink<T> createKafkaSink(Properties outputProperties, SerializationSchema<T> recordKeySerializationSchema, TopicSelector<T> topicSelector) {
        ConfigValidator.validate(outputProperties, "lob", "bootstrap.servers");

        String bootstrapServers = outputProperties.getProperty("bootstrap.servers");

        KafkaRecordSerializationSchema<T> recordSerializationSchema = KafkaRecordSerializationSchema.<T>builder()
                                                                                                    .setTopicSelector(topicSelector)
                                                                                                    // Use a field as kafka record key
                                                                                                    // Define no keySerializationSchema to publish kafka records with no key
                                                                                                    .setKeySerializationSchema(recordKeySerializationSchema)
                                                                                                    // Serialize the Kafka record value (payload) as JSON
                                                                                                    .setValueSerializationSchema(new JsonSerializationSchema<>())
                                                                                                    .build();


        return KafkaSink.<T>builder().setBootstrapServers(bootstrapServers).setKafkaProducerConfig(outputProperties)
                        .setRecordSerializer(recordSerializationSchema)
//                        .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                        .build();
    }
}