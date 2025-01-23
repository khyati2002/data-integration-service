package com.salescode.dis.config;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import com.applicate.services.channelkart.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Component
public class S3YamlPropertyLoader implements EnvironmentPostProcessor {

    Logger log = LoggerFactory.getLogger(S3YamlPropertyLoader.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, org.springframework.boot.SpringApplication application) {
        if ("local".equals(environment.getProperty("spring.profiles.active"))) {
            log.info("Using local environment");
            return;
        }
        String lob = environment.getProperty("app.lob");
        if (lob == null) {
            throw new IllegalArgumentException("app.lob is required");
        }
        String s3Uri = StringUtils.format(environment.getProperty("config.s3.uri"), lob);
        String region = environment.getProperty("config.s3.region");
        if (s3Uri == null || region == null) {
            throw new IllegalArgumentException("s3.uri and s3.region must be specified");
        }
        S3Client s3Client = null;
        try {
            URI uri = new URI(s3Uri);
            String bucketName = uri.getHost();
            String key = uri.getPath().substring(1); // Remove leading "/"

            s3Client = S3Client.builder().credentialsProvider(DefaultCredentialsProvider.create()).region(Region.of(region)).build();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(key).build();

            ResponseBytes<?> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            InputStream inputStream = objectBytes.asInputStream();
            Resource resource = new InputStreamResource(inputStream);

            YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
            List<org.springframework.core.env.PropertySource<?>> propertySourcesFromYaml = loader.load("s3YamlProperties", resource);
            PropertySource<?> s3PropertySource = propertySourcesFromYaml.get(0);
            MutablePropertySources propertySources = environment.getPropertySources();
            propertySources.addLast(s3PropertySource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration from S3", e);
        } finally {
            if (s3Client != null) {
                s3Client.close();
            }
        }
    }
}