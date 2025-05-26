package com.salescode.dis.insights.sdk.manager;

import com.salescode.dis.insights.sdk.InsightsEnv;
import lombok.Getter;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

public class InsightsBuilder {

    private final RestTemplate restTemplate = new RestTemplate();

    @Getter
    private InsightsEnv env;

    private InsightsBuilder() {
    }

    public static InsightsBuilder builder() {
        return new InsightsBuilder();
    }

    public InsightsBuilder withEnv(final InsightsEnv env) {
        Objects.requireNonNull(env, "InsightsEnv cannot be null");
        this.env = env;
        return this;
    }

    public InsightsBuilder withEnv(final String env) {
        Objects.requireNonNull(env, "Environment string cannot be null");
        this.env = InsightsEnv.valueOf(env.toUpperCase().trim());
        return this;
    }

    public InsightsBuilder withTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative");
        }
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restTemplate.setRequestFactory(requestFactory);
        return this;
    }

    public InsightsManager build() {
        if (this.env == null) {
            throw new IllegalStateException("Environment must be set before building InsightsManager");
        }
        return new InsightsManager(restTemplate, env);
    }

    public SafeInsightsManager buildSafe() {
        if (this.env == null) {
            throw new IllegalStateException("Environment must be set before building SafeInsightsManager");
        }
        InsightsManager coreManager = new InsightsManager(restTemplate, env);
        return new SafeInsightsManager(coreManager);
    }
}