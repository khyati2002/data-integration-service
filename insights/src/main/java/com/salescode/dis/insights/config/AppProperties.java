package com.salescode.dis.insights.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private List<String> activeEnvironments;

    public List<String> getActiveEnvironments() {
        return activeEnvironments;
    }

    public void setActiveEnvironments(List<String> activeEnvironments) {
        this.activeEnvironments = activeEnvironments;
    }
}
