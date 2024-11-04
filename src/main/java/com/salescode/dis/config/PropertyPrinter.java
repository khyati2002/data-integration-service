package com.salescode.dis.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PropertyPrinter implements CommandLineRunner {

    @Autowired
    private ConfigurableEnvironment environment;

    @Override
    public void run(String... args) {
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            System.out.println("Property Source: " + propertySource.getName());
            if (propertySource.getSource() instanceof Map) {
                Map<String, Object> properties = (Map<String, Object>) propertySource.getSource();
                properties.forEach((key, value) -> System.out.println(key + " = " + value));
            }
        }
        System.out.println("Done");
    }
}
