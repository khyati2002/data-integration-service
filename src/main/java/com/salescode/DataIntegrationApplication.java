package com.salescode;

import com.salescode.dataintegration.scanner.ExternalRegistryScanner;
import com.salescode.dis.config.DatabaseConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;

@SpringBootApplication
@Import({DatabaseConfig.class})
@ComponentScan(basePackages = {"com.salescode.dataintegration", "com.salescode.channelkart"})
public class DataIntegrationApplication {

    private final ExternalRegistryScanner externalRegistryScanner;
    private final Environment environment;

    public DataIntegrationApplication(ExternalRegistryScanner externalRegistryScanner, Environment environment) {
        this.externalRegistryScanner = externalRegistryScanner;
        this.environment = environment;
    }

    @PostConstruct
    void init() {
        externalRegistryScanner.loadClassesFromLob(environment.getProperty("app.lob"));
    }
}