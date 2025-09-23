package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.service.InsightsMetadataService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cache")
public class CacheController {

    private final InsightsMetadataService metadataService;

    public CacheController(InsightsMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @DeleteMapping("/queries/{key}")
    public String evictQuery(@PathVariable String key) {
        metadataService.evictQuery(key+"-redshift_query");
        return "Cache evicted for key: " + key;
    }

    @DeleteMapping("/queries")
    public String evictAllQueries() {
        metadataService.evictAllQueries();
        return "All queries evicted from cache.";
    }
}
