package com.salescode.dis.insights.config;

import ai.salescode.observability.toolkit.api.AuthenticationContext;
import org.springframework.stereotype.Component;

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
        return "trace-id-12";
    }

    @Override
    public Map<String, String> getAdditionalAttributes() {
        return Map.of("region", "us-east-1");
    }
}
