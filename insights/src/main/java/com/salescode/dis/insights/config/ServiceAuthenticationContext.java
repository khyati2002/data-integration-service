package com.salescode.dis.insights.config;

import ai.salescode.observability.toolkit.api.AuthenticationContext;
import org.springframework.stereotype.Component;
import java.util.UUID;

import java.util.Map;

@Component
public class ServiceAuthenticationContext implements AuthenticationContext {

    @Override
    public String getUserName() {
        return "integration_user";
    }

    @Override
    public String getLob() {
        return "default";
    }

    @Override
    public String getTraceId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public Map<String, String> getAdditionalAttributes() {
        return Map.of("region", "ap-south-1");
    }
}
