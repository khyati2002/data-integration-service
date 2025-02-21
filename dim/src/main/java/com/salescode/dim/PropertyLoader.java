package com.salescode.dim;

import com.amazonaws.services.kinesisanalytics.runtime.KinesisAnalyticsRuntime;
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class PropertyLoader {

    public static final String LOCAL_APPLICATION_PROPERTIES_RESOURCE = "flink-application-properties-dev.json";
    private static final Logger LOG = LoggerFactory.getLogger(PropertyLoader.class);

    private static boolean isLocal(StreamExecutionEnvironment env) {
        return env == null || env instanceof LocalStreamEnvironment;
    }

    /**
     * Load application properties from Amazon Managed Service for Apache Flink runtime or from a local resource, when the environment is local
     */
    public static Map<String, Properties> loadApplicationProperties(StreamExecutionEnvironment env) throws IOException {
        Map<String, Properties> appProperties = new HashMap<>();
        if (isLocal(env)) {
            LOG.info("Loading application properties from '{}'", LOCAL_APPLICATION_PROPERTIES_RESOURCE);
            URL resource = Objects.requireNonNull(PropertyLoader.class.getClassLoader()
                                                                     .getResource(LOCAL_APPLICATION_PROPERTIES_RESOURCE));
            appProperties = KinesisAnalyticsRuntime.getApplicationProperties(resource.getPath());
            return overrideWithPropertiesFile(appProperties);
        } else {
            LOG.info("Loading application properties from Amazon Managed Service for Apache Flink");
            return KinesisAnalyticsRuntime.getApplicationProperties();
        }
    }


    private static Map<String, Properties> overrideWithPropertiesFile(Map<String, Properties> appProperties) {
        Properties overrideProps = loadPropertiesFromFile();
        // Override keys in each property group only if they exist in the override file
        appProperties.forEach((groupName, groupProps) -> overrideProps.stringPropertyNames().forEach(key -> {
            if (groupProps.containsKey(key)) {
                String overrideValue = overrideProps.getProperty(key);
                groupProps.setProperty(key, overrideValue);
                LOG.debug("Overwriting group '{}': setting key '{}' to value '{}'", groupName, key, overrideValue);
            }
        }));
        return appProperties;
    }

    private static Properties loadPropertiesFromFile() {
        Properties props = new Properties();
        try (InputStream in = PropertyLoader.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                LOG.info("Loading override properties from 'application.properties'");
                props.load(in);
            } else {
                LOG.warn("Override properties file 'application.properties' not found.");
            }
        } catch (IOException e) {
            LOG.error("Error loading override properties", e);
        }
        return props;
    }

    public static Properties mergeProperties(Properties properties, Properties authProperties) {
        properties.putAll(authProperties);
        return properties;
    }
}
