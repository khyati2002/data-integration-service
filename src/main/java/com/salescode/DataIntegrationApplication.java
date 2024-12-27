package com.salescode;

import com.salescode.channelkart.services.SpringContext;
//import com.salescode.dis.config.DatabaseConfig;
import com.salescode.dis.config.DatabaseConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.annotation.PostConstruct;
import java.util.Optional;

@SpringBootApplication
@Import({DatabaseConfig.class})
@ComponentScan(basePackages = {"com.salescode.dataintegration", "com.salescode.channelkart"})
@EnableJpaRepositories({"com.salescode.channelkart"})
@EntityScan(basePackages = {"com.salescode.channelkart"})
public class DataIntegrationApplication {

  //  private final ExternalRegistryScanner externalRegistryScanner;
    private final Environment environment;

    public DataIntegrationApplication(
            Environment environment
    ) {
        this.environment = environment;
    }

    @PostConstruct
    void init() {
       // externalRegistryScanner.loadClassesFromLob(environment.getProperty("app.lob"));
    }
}