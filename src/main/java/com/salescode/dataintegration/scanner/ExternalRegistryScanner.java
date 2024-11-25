package com.salescode.dataintegration.scanner;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.salescode.channelkart.utils.StringUtils;
import com.salescode.dataintegration.etl.interfaces.TypeAwareEtlStep;
import com.salescode.jooq.generated.tables.pojos.Profile;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.salescode.jooq.generated.tables.Profile.*;

@Slf4j
@Service
public class ExternalRegistryScanner {

    public static final String LOB_PROPERTIES_PATH = "https://dataplatform-flink.s3.ap-south-1.amazonaws.com/properties/{lob}/dis-bundle.jar";

    // Cache for storing class instances
    private final Map<String, ? extends TypeAwareEtlStep> instanceCache = new HashMap<>();

    /**
     * Loads and caches classes from a bundle jar file based on the LOB.
     *
     * @param lob the LOB of the client.
     */
    public void loadClassesFromLob(String lob) {
        String s3JarUrl = StringUtils.format(LOB_PROPERTIES_PATH, lob);
        String s3JarUrlPreSigned = generatePresignedUrl(s3JarUrl, TimeUnit.DAYS.toMillis(7)).toString();
        JarScanner jarScanner = new JarScanner();
        jarScanner.loadAndCacheClasses(s3JarUrlPreSigned, instanceCache);
    }

    public static URL generatePresignedUrl(String path, long expiration) {
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();
        AmazonS3URI s3URI = new AmazonS3URI(path);
        long expirationTime = System.currentTimeMillis()+expiration;
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

    public Collection< ? extends TypeAwareEtlStep> getInstances(){
        return instanceCache.values();
    }
}