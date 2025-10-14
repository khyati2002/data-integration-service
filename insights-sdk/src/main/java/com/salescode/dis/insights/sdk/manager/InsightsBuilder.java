package com.salescode.dis.insights.sdk.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.salescode.dis.insights.sdk.InsightsEnv;
import lombok.Getter;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class InsightsBuilder {

    private final RestTemplate restTemplate = new RestTemplate();

    @Getter
    private InsightsEnv env;

    private Cache<String, Object> localCache;

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
        try {
            this.env = InsightsEnv.valueOf(env.toUpperCase().trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown environment '" + env);
        }
        return this;
    }

    public InsightsBuilder withLocalCache(boolean enableCache,long expiryTime) {
        if (enableCache) {
            this.localCache = Caffeine.newBuilder()
                    .maximumSize(10000)
                    .expireAfterAccess(expiryTime, TimeUnit.MINUTES)
                    .initialCapacity(100)
                    .recordStats()
                    .build();
        } else {
            this.localCache = null;
        }
        return this;
    }

    public InsightsBuilder withLocalCache() {
        return withLocalCache(true,1);
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
        return new InsightsManager(restTemplate, env, localCache);
    }

    public SafeInsightsManager buildSafe() {
        if (this.env == null) {
            throw new IllegalStateException("Environment must be set before building SafeInsightsManager");
        }
        InsightsManager coreManager = new InsightsManager(restTemplate, env, localCache);
        return new SafeInsightsManager(coreManager);
    }
}