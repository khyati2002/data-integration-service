package com.salescode.dis.flink.sources;

import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.json.JsonDeserializationSchema;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DISKafkaSourceBuilder {

    @Value("${app.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.producer-txn-timeout}")
    private String producerTxnTimeOut;

    @Value("${app.jobs.from-kafka-to-db.main-topic-name}")
    private String subscribedTopic;

    @Value("${app.jobs.from-kafka-to-db.consumer-group-id}")
    private String consumerGroupId;
    

    public KafkaSource<ObjectNode> build(){
        return KafkaSource.<ObjectNode>builder()
            .setBootstrapServers(bootstrapServers)
            .setTopics(subscribedTopic)
            .setGroupId(consumerGroupId)
            .setStartingOffsets(OffsetsInitializer.latest())
            .setValueOnlyDeserializer(new JsonDeserializationSchema<ObjectNode>(ObjectNode.class))
            .build();
    }
}
