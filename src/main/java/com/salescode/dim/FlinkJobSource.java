package com.salescode.dim;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class FlinkJobSource {

    private static final OffsetsInitializer DEFAULT_OFFSETS_INITIALIZER = OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST);
    private static Properties inputProperties;

    private static final Logger LOG = LoggerFactory.getLogger(FlinkJobSource.class);

    public static <T> KafkaSource<T> createKafkaSource(Properties inputProperties, final DeserializationSchema<T> valueDeserializationSchema) {
        LOG.info(String.valueOf(inputProperties));
        FlinkJobSource.inputProperties = inputProperties;
        // Validate required properties
        validate(inputProperties, "bootstrap.servers", "topic", "group.id", "lob");

        LOG.info("Validation Sucess");

        // Determine the starting offsets initializer
        OffsetsInitializer startingOffsetsInitializer = inputProperties.containsKey("startTimestamp") ? OffsetsInitializer.timestamp(Long.parseLong(inputProperties.getProperty("startTimestamp"))) : DEFAULT_OFFSETS_INITIALIZER;

//        String lobTopicName = inputProperties.getProperty("lob") + inputProperties.getProperty("topic");
        String lobTopicName = inputProperties.getProperty("topic");

        return KafkaSource.<T>builder()
                          .setBootstrapServers(inputProperties.getProperty("bootstrap.servers"))
                          .setTopics(lobTopicName)
                          .setGroupId(inputProperties.getProperty("group.id"))
                          .setStartingOffsets(startingOffsetsInitializer) // Used when the application starts with no state
                          .setValueOnlyDeserializer(valueDeserializationSchema)
                          .setProperties(inputProperties)
                          .build();
    }

    public static void validate(Properties props, String... requiredKeys) {
        for (String key : requiredKeys) {
            if (!props.containsKey(key.trim())) {
                throw new IllegalArgumentException("Missing required Kafka property: " + key);
            }
        }
    }

    /**
     * Validates the required Kafka configuration properties.
     *
     * @param properties Kafka properties to be validated.
     */

}