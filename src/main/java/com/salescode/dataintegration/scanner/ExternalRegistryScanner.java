package com.salescode.dataintegration.scanner;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.salescode.channelkart.utils.StringUtils;
import com.salescode.dataintegration.etl.interfaces.TypeAwareEtlStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ExternalRegistryScanner {

    // Cache for storing class instances
    private final Map<String, ? extends TypeAwareEtlStep> instanceCache = new HashMap<>();
    private final Environment environment;

    public ExternalRegistryScanner(Environment environment) {
        this.environment = environment;
    }

    /**
     * Loads and caches classes from a bundle jar file based on the LOB.
     *
     * @param lob the LOB of the client.
     */
    public void loadClassesFromLob(String lob) {
        String bundleUri = environment.getProperty("app.bundle.uri");
        if (bundleUri == null) {
            log.info("Bundle URI not found for lob {}", lob);
            return;
        }
        String s3JarUrl = StringUtils.format(bundleUri, lob);
        String s3JarUrlPreSigned = generatePresignedUrl(s3JarUrl, TimeUnit.DAYS.toMillis(7)).toString();
        JarScanner jarScanner = new JarScanner();
        jarScanner.loadAndCacheClasses(s3JarUrlPreSigned, instanceCache);
    }

    public URL generatePresignedUrl(String path, long expiration) {
        String region = environment.getProperty("config.s3.region");
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(region).build();
        AmazonS3URI s3URI = new AmazonS3URI(path);
        long expirationTime = System.currentTimeMillis() + expiration;
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(s3URI.getBucket(), s3URI.getKey())
                .withMethod(com.amazonaws.HttpMethod.GET)
                .withExpiration(new Date(expirationTime));
        return s3Client.generatePresignedUrl(request);
    }

    /**
     * Retrieve a cached instance of a class by its name.
     */
    public Object getCachedInstance(String className) {
        return instanceCache.get(className);
    }

    public Collection<? extends TypeAwareEtlStep> getInstances() {
        return instanceCache.values();
    }
}