package com.salescode.dis.insights.config;

import com.salescode.dis.insights.config.AppProperties;
import com.salescode.dis.insights.service.PropertyService;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Configuration
@Profile("!test")
public class PropertyInitializer {

    private final PropertyService propertyService;
    private final AppProperties appProperties;

    public PropertyInitializer(PropertyService propertyService, AppProperties appProperties) {
        this.propertyService = propertyService;
        this.appProperties = appProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        for (String env : appProperties.getActiveEnvironments()) {
            propertyService.fetchAndCacheFeaturesForEnv(env);
        }
    }
}
