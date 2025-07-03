package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.service.PropertyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    // GET /api/properties/{env}/{lob}
    @GetMapping("/insights-enabled/{env}/{lob}")
    public ResponseEntity<Boolean> getProperty(@PathVariable String env, @PathVariable String lob) {
        Boolean enabled = propertyService.isInsightsEnabled(lob);

        if (enabled == null) {
            // Not cached, so fetch and retry
            try {
                String baseUrl = "https://" + env + ".salescode.ai";
                propertyService.fetchAndCacheFeatureForLob(baseUrl, lob);
                enabled = propertyService.isInsightsEnabled(lob);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().build();
            }
        }
        return ResponseEntity.ok(enabled);
    }


    @GetMapping("/env/{lob}")
    public ResponseEntity<String> getEnvironment(@PathVariable String lob) {
        String environment = propertyService.getEnvFromLob(lob);
        if (environment != null) {
            return ResponseEntity.ok(environment);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/properties/cache/{lob}
    @DeleteMapping("/cache/{lob}")
    public ResponseEntity<String> clearCache(@PathVariable String lob) {
        propertyService.evictLobFromCache(lob);
        return ResponseEntity.ok("Cache cleared for LOB: " + lob);
    }

    // DELETE /api/properties/cache
    @DeleteMapping("/cache")
    public ResponseEntity<String> clearAllCache() {
        propertyService.clearAllCache();
        return ResponseEntity.ok("All LOB feature cache cleared.");
    }

    @DeleteMapping("/cache/property")
    public ResponseEntity<String> clearAllCacheProperty() {
        propertyService.clearAllPropertycache();
        return ResponseEntity.ok("All LOB feature cache cleared.");
    }
}
