package com.salescode.dim;

import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.util.Properties;

public class FlinkJobSource {

    private static final OffsetsInitializer DEFAULT_OFFSETS_INITIALIZER = OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST);

    public static <T> KafkaSource<T> createKafkaSource(Properties inputProperties, final KafkaRecordDeserializationSchema<T> valueDeserializationSchema, String topic) {
        // Validate required properties
        ConfigValidator.validate(inputProperties, "bootstrap.servers", "group.id");

        String bootstrapServers = inputProperties.getProperty("bootstrap.servers");

        KafkaTopicCreator.createTopicIfNotExists(topic, bootstrapServers);

        // Determine the starting offsets initializer
        OffsetsInitializer startingOffsetsInitializer = OffsetsInitializer.earliest();

        return KafkaSource.<T>builder().setBootstrapServers(bootstrapServers)
                          .setTopics(topic)
                          .setGroupId(inputProperties.getProperty("group.id"))
                          .setStartingOffsets(startingOffsetsInitializer) // Used when the application starts with no state
                          .setDeserializer(valueDeserializationSchema)
                          .setProperties(inputProperties)
                          .build();
    }

}