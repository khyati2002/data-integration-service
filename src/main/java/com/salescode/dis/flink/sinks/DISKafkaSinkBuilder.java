package com.salescode.dis.flink.sinks;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DISKafkaSinkBuilder {

    @Value("${app.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.producer-txn-timeout}")
    private String producerTxnTimeOut;


    public KafkaSink<String> build(String topic){
        return KafkaSink.<String>builder()
            .setBootstrapServers(bootstrapServers)
            .setProperty("transaction.timeout.ms", producerTxnTimeOut)
            .setRecordSerializer(
                    KafkaRecordSerializationSchema.builder()
                            .setTopic(topic)
                            .setValueSerializationSchema(new SimpleStringSchema())
                            .build()
            )
            .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
            .build();
    }
}