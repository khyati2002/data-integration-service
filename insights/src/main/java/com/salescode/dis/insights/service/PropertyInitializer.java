package com.salescode.dis.insights.service;

import com.salescode.dis.insights.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PropertyInitializer {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private AppProperties appProperties;

    @PostConstruct
    public void init() {
        for (String env : appProperties.getActiveEnvironments()) {
            propertyService.fetchAndCacheFeaturesForEnv(env);
        }
    }
}
