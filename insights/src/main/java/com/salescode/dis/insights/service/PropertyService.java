package com.salescode.dis.insights.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dis.insights.entity.InsightsMetadata;
import com.salescode.dis.insights.repository.InsightsMetadataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PropertyService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private InsightsMetadataRepository metadataRepository;
    private final Cache<String, Boolean> lobFeatureCache; // key = lob:env
    private final Cache<String, String> lobToEnvCache;       // key = lob, value = env

    private String TOKEN ;
    private String getToken() {
        if (TOKEN == null) {
            TOKEN = metadataRepository.findByKey("token")
                    .map(InsightsMetadata::getValue)
                    .orElseThrow(() -> new RuntimeException("Token not found in DB"));
        }
        return TOKEN;
    }

    public PropertyService(RestTemplateBuilder builder, InsightsMetadataRepository insightsMetadataRepository) {
        this.restTemplate = builder.build();
        this.objectMapper = new ObjectMapper();
        this.metadataRepository = insightsMetadataRepository;
        this.lobFeatureCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .build();

        this.lobToEnvCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .build();
    }

    public String getEnvFromLob(String lob) {
        return lobToEnvCache.getIfPresent(lob);
    }

    public String getBaseUrl(String env){
        return metadataRepository.findByKey(env)
                .map(InsightsMetadata::getValue)
                .orElseThrow(() -> new IllegalArgumentException("Base URL not found for environment: " + env));
    }

    public void fetchAndCacheFeaturesForEnv(String env) {
        String baseUrl = getBaseUrl(env);

        String healthCheckUrl = baseUrl + "/hckeck";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(healthCheckUrl, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode lobNames = root.path("systemInfo").path("lobNames");

            if (lobNames.isArray()) {
                for (JsonNode lobNode : lobNames) {
                    String lob = lobNode.asText();
                    lobToEnvCache.put(lob, env); // Cache lob -> env
                    fetchAndCacheFeatureForLob(baseUrl, lob);
                }
            }

        } catch (Exception e) {
        }
    }

    public void fetchAndCacheFeatureForLob(String baseUrl, String lob) {
        String propertyUrl = baseUrl + "/v1/properties?name=enable.insights.integration";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + getToken());
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
                        lobFeatureCache.put(lob , enabled);
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to fetch property data for LOB: " + lob + ". Error: " + e.getMessage());
        }
    }

    public List<String> getLobsForEnv(String env) {
        return lobToEnvCache
                .asMap()
                .entrySet().stream()
                .filter(e -> env.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public Boolean isInsightsEnabled(String lob) {
        Boolean enabled = lobFeatureCache.getIfPresent(lob);
        if(enabled!=null){
            String env = lobToEnvCache.getIfPresent(lob);
        }
        return enabled;
    }

    public void evictLobFromCache(String lob) {
        String env = lobToEnvCache.getIfPresent(lob);
        if (env != null) {
            lobFeatureCache.invalidate(lob);
        }
    }

    public void clearAllPropertycache(){
        lobFeatureCache.invalidateAll();
    }

    public void clearAllCache() {
        lobFeatureCache.invalidateAll();
        lobToEnvCache.invalidateAll();
    }
}
