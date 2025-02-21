package com.salescode.dim;

import java.util.Properties;

public class ConfigValidator {

    /**
     * Validates common Kafka properties for both Source and Sink.
     *
     * @param properties Kafka configuration properties.
     * @param requiredKeys Keys that must be present in the properties.
     */
    public static void validate(Properties properties, String... requiredKeys) {
        for (String key : requiredKeys) {
            if (!properties.containsKey(key) || properties.getProperty(key).trim().isEmpty()) {
                throw new IllegalArgumentException("Missing required Kafka property: " + key);
            }
        }
    }

}