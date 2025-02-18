package com.salescode;

import com.salescode.dataintegration.etl.interfaces.TypeAwareEtlStep;
import com.salescode.dataintegration.scanner.ExternalRegistryScanner;
import com.salescode.dataintegration.scanner.S3JarClassLoader;
import com.salescode.dis.config.DatabaseConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@Import({DatabaseConfig.class})
@ComponentScan(basePackages = {"com.salescode.dataintegration", "com.salescode.channelkart"})
public class DataIntegrationApplication {

    private final S3JarClassLoader s3JarClassLoader;
    private final Environment environment;
    private final Map<String,? extends TypeAwareEtlStep> instanceCache = new HashMap<>();

    public DataIntegrationApplication(Environment environment, S3JarClassLoader s3JarClassLoader) {
       this.s3JarClassLoader = s3JarClassLoader;
        this.environment = environment;
    }

    @PostConstruct
    void init() throws IOException, ClassNotFoundException {
     // s3JarClassLoader.scanAndCacheClassesFromS3("salescode-dev-uat","dataintegration/ckuatunnati/dis-bundle.jar",instanceCache);
    }
}