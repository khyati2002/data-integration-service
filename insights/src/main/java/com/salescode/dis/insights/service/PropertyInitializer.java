package com.salescode.dis.insights.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PropertyInitializer {

    @Autowired
    private PropertyService propertyService;

    @PostConstruct
    public void init() {
        propertyService.fetchAndCacheFeaturesForEnv("dev");
        propertyService.fetchAndCacheFeaturesForEnv("uat");
    }
}
