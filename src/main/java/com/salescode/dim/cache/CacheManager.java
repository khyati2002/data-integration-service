// 3. CacheManager to handle multiple caches
package com.salescode.dim.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class CacheManager {
    private static final CacheManager INSTANCE = new CacheManager();

    private final Map<String, Cache<String, Object>> caches = new ConcurrentHashMap<>();

    private CacheManager() {
    }

    public static CacheManager getInstance() {
        return INSTANCE;
    }

    public Cache<String, Object> getCache(String name, int maximumSize, int expireAfterMinutes) {
        return caches.computeIfAbsent(name, cacheName ->
                Caffeine.newBuilder()
                        .maximumSize(maximumSize)
                        .expireAfterWrite(expireAfterMinutes, TimeUnit.MINUTES)
                        .recordStats()
                        .build()
        );
    }

    public void evictAll(String cacheName) {
        Cache<String, Object> cache = caches.get(cacheName);
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    public void evictByPattern(String cacheName, String keyPattern) {
        Cache<String, Object> cache = caches.get(cacheName);
        if (cache != null && keyPattern != null && !keyPattern.isEmpty()) {
            Pattern pattern = Pattern.compile(keyPattern);
            cache.asMap().keySet().stream()
                    .filter(key -> pattern.matcher(key).matches())
                    .forEach(cache::invalidate);
        }
    }

    public Map<String, Cache<String, Object>> getAllCaches() {
        return caches;
    }

    public void printStats() {
        caches.forEach((name, cache) -> {
            if (cache.stats() != null) {
                System.out.println("Cache: " + name);
                System.out.println("  Hit rate: " + cache.stats().hitRate());
                System.out.println("  Miss rate: " + cache.stats().missRate());
                System.out.println("  Request count: " + cache.stats().requestCount());
                System.out.println("  Size: " + cache.estimatedSize());
            }
        });
    }
}
