package com.salescode.dis.insights.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PropertyService {


    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Cache<String, Boolean> lobFeatureCache;

    private final String TOKEN = "hardcoded_token";

    public PropertyService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
        this.objectMapper = new ObjectMapper();

        // Configure Caffeine cache with optional expiry or size limits
        this.lobFeatureCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .build();
    }

    public void fetchAndCacheFeaturesForEnv(String env) {
        String baseUrl;
        switch (env.toLowerCase()) {
            case "dev" -> baseUrl = "https://dev.salescode.ai";
            case "uat" -> baseUrl = "https://uat.salescode.ai";
            default -> throw new IllegalArgumentException("Invalid environment: " + env);
        }

        String healthCheckUrl = baseUrl + "/hckeck";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(healthCheckUrl, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode lobNames = root.path("systemInfo").path("lobNames");

            if (lobNames.isArray()) {
                for (JsonNode lobNode : lobNames) {
                    String lob = lobNode.asText();
                    fetchAndCacheFeatureForLob(baseUrl, lob);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch LOBs from health check: " + e.getMessage(), e);
        }
    }

    public void fetchAndCacheFeatureForLob(String baseUrl, String lob) {
        String propertyUrl = baseUrl + "/v1/properties?name=enable.insights.integration";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + TOKEN);
        headers.set("lob", lob);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    propertyUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode features = root.path("features");

            if (features.isArray()) {
                for (JsonNode feature : features) {
                    if ("enable.insights.integration".equals(feature.path("name").asText())) {
                        String value = feature.path("value").asText();
                        boolean enabled = Boolean.parseBoolean(value);
                        lobFeatureCache.put(lob, enabled);
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to fetch property data for LOB: " + lob + ". Error: " + e.getMessage());
        }
    }

    // Evict a specific LOB from the cache
    public void evictLobFromCache(String lob) {
        lobFeatureCache.invalidate(lob);
    }

    // Clear the entire cache
    public void clearAllCache() {
        lobFeatureCache.invalidateAll();
    }


    public Boolean isInsightsEnabled(String lob) {
        Boolean enabled = lobFeatureCache.getIfPresent(lob);
        return enabled != null && enabled;
    }
}
