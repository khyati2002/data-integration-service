package com.salescode.dim;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.util.Properties;

public class FlinkJobSource {

    private static final OffsetsInitializer DEFAULT_OFFSETS_INITIALIZER = OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST);

    public static <T> KafkaSource<T> createKafkaSource(Properties inputProperties, final DeserializationSchema<T> valueDeserializationSchema) {
        // Validate required properties
        ConfigValidator.validate(inputProperties, "bootstrap.servers", "input.topic", "group.id", "lob");

        // Determine the starting offsets initializer
        OffsetsInitializer startingOffsetsInitializer = inputProperties.containsKey("startTimestamp") ? OffsetsInitializer.timestamp(Long.parseLong(inputProperties.getProperty("startTimestamp"))) : DEFAULT_OFFSETS_INITIALIZER;

        return KafkaSource.<T>builder()
                          .setBootstrapServers(inputProperties.getProperty("bootstrap.servers"))
                          .setTopics(inputProperties.getProperty("input.topic"))
                          .setGroupId(inputProperties.getProperty("group.id"))
                          .setStartingOffsets(startingOffsetsInitializer) // Used when the application starts with no state
                          .setValueOnlyDeserializer(valueDeserializationSchema)
                          .setProperties(inputProperties)
                          .build();
    }

    /**
     * Validates the required Kafka configuration properties.
     *
     * @param properties Kafka properties to be validated.
     */

}