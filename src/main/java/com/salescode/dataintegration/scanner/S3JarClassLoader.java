package com.salescode.dataintegration.scanner;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.salescode.channelkart.converters.JSONObjectConverter;
import com.salescode.dataintegration.etl.interfaces.TypeAwareEtlStep;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.HashMap;
import java.util.Map;


@Component
public class S3JarClassLoader extends ClassLoader {
    private final Map<String, byte[]> classBytes = new HashMap<>();

    public static final String REGION = "ap-south-1";

    private final DSLContext dsl;

    @Autowired
    private JSONObjectConverter jsonObjectConverter;

    public S3JarClassLoader(DSLContext dsl) {
        this.dsl = dsl;
    }


    public void scanAndCacheClassesFromS3(String bucketName, String jarKey, Map instanceCache) {
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("", "")))
                .withRegion(REGION)
                .build();

        try (S3Object s3Object = s3Client.getObject(bucketName, jarKey);
             S3ObjectInputStream s3InputStream = s3Object.getObjectContent()) {

            // Use a JarInputStream to read the JAR content
            try (JarInputStream jarInputStream = new JarInputStream(s3InputStream)) {
                JarEntry entry;
                while ((entry = jarInputStream.getNextJarEntry()) != null) {
                    if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                        String className = entry.getName();
                        if (className.startsWith("BOOT-INF/classes/")) {
                            className = className.substring("BOOT-INF/classes/".length());
                        }
                        className = className.replace("/", ".").replace(".class", "");

                        // Use a custom class loader to load classes directly from the input stream
                            try {
                                // Create a custom class loader using the JarInputStream
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                while ((bytesRead = jarInputStream.read(buffer)) != -1) {
                                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                                }

                                // Create a custom class loader
                                String finalClassName = className;
                                ClassLoader classLoader = new ClassLoader() {
                                    @Override
                                    public Class<?> findClass(String name) throws ClassNotFoundException {
                                        // Read the class data and define the class
                                        if (name.equals(finalClassName)) {
                                            byte[] classData = byteArrayOutputStream.toByteArray();
                                            return defineClass(name, classData, 0, classData.length);
                                        }
                                        throw new ClassNotFoundException(name);
                                    }
                                };

                                // Load the class using the custom class loader
                                Class<?> clazz = classLoader.loadClass(className);
                                TypeAwareEtlStep instance = (TypeAwareEtlStep) clazz.getDeclaredConstructor().newInstance();
                                instanceCache.put(className, instance);
                                // logger.info("Loaded and cached class: {}", className);
                            } catch (Exception e) {
                                // logger.error("Error loading class: {}", className, e);
                            }

                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error scanning JAR file from S3", e);
        }
    }

}
