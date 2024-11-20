package com.salescode;

import com.salescode.dis.config.DatabaseConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({DatabaseConfig.class})
@ComponentScan(basePackages = {"com.salescode.dataintegration", "com.salescode.channelkart"})
public class DataIntegrationApplication {

}