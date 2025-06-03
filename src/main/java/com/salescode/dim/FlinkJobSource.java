package com.salescode.dim;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.util.Properties;

public class FlinkJobSource {

    private static final OffsetsInitializer DEFAULT_OFFSETS_INITIALIZER = OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST);

    public static <T> KafkaSource<T> createKafkaSource(Properties inputProperties, final DeserializationSchema<T> valueDeserializationSchema, String topic) {
        // Validate required properties
        ConfigValidator.validate(inputProperties, "bootstrap.servers", "group.id");

        String bootstrapServers = inputProperties.getProperty("bootstrap.servers");

        KafkaTopicCreator.createTopicIfNotExists(topic, bootstrapServers, 5, (short) 1);

        // Determine the starting offsets initializer
        OffsetsInitializer startingOffsetsInitializer = inputProperties.containsKey("startTimestamp") ? OffsetsInitializer.timestamp(Long.parseLong(inputProperties.getProperty("startTimestamp"))) : DEFAULT_OFFSETS_INITIALIZER;
//        OffsetsInitializer startingOffsetsInitializer = OffsetsInitializer.earliest();

        return KafkaSource.<T>builder().setBootstrapServers(bootstrapServers)
                          .setTopics(topic)
                          .setGroupId(inputProperties.getProperty("group.id"))
                          .setStartingOffsets(startingOffsetsInitializer) // Used when the application starts with no state
                          .setValueOnlyDeserializer(valueDeserializationSchema)
                          .setProperties(inputProperties)
                          .build();
    }

}