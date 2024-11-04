package com.salescode.dis.config;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
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

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, org.springframework.boot.SpringApplication application) {
        String s3Uri = environment.getProperty("config.s3.uri");
        String region = environment.getProperty("config.s3.region");

        if (s3Uri == null || region == null) {
            throw new IllegalArgumentException("s3.uri and s3.region must be specified");
        }

        S3Client s3Client = null;
        try {
            URI uri = new URI(s3Uri);
            String bucketName = uri.getHost();
            String key = uri.getPath().substring(1); // Remove leading "/"

            s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseBytes<?> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            InputStream inputStream = objectBytes.asInputStream();
            Resource resource = new InputStreamResource(inputStream);

            YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
            List<org.springframework.core.env.PropertySource<?>> propertySourcesFromYaml = loader.load("s3YamlProperties", resource);

            MutablePropertySources propertySources = environment.getPropertySources();
            propertySources.addFirst(propertySourcesFromYaml.get(0));
  
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration from S3", e);
        } finally {
            if (s3Client != null) {
                s3Client.close();
            }
        }
    }
}