// 3. CacheManager to handle multiple caches
package com.salescode.dim.cache;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RFuture;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
public class CacheManager {
    private static final CacheManager INSTANCE = new CacheManager();

    private Map<String, RMapCache<String, Object>> caches =new ConcurrentHashMap<>();

    private CacheManager() {
    }

    private static RedissonClient redissonClient;
    static {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");
        redissonClient = Redisson.create(config);
    }

    public static CacheManager getInstance() {
        return INSTANCE;
    }

    public RMapCache<String, Object> getCache(String name, int expireAfterMinutes) {
        return caches.computeIfAbsent(name, cacheName -> {
            RMapCache<String, Object> mapCache = redissonClient.getMapCache(cacheName);
            return mapCache;
        });
    }

    public void evictAll(String cacheName) {
        RMapCache<String, Object> cache = caches.get(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    public void evictByPattern(String cacheName, String keyPattern) {
        RMapCache<String, Object> cache = caches.get(cacheName);
        if (cache != null && keyPattern != null && !keyPattern.isEmpty()) {
            Pattern pattern = Pattern.compile(keyPattern);
            cache.keySet().stream()
                    .filter(key -> pattern.matcher(key).matches())
                    .forEach(cache::remove);
        }
    }


    public Map<String, RMapCache<String, Object>> getAllCaches() {
        return caches;
    }

    public void printStats() {
        caches.forEach((name, cache) -> {
            RFuture<Set<Map.Entry<String, Object>>> setRFuture = cache.readAllEntrySetAsync();
            setRFuture.thenAccept(entrySet -> {
                log.info("Cache: {}", name);
                log.info("  Size: {}", entrySet.size());
            }).exceptionally(ex -> {
                log.info("Error retrieving cache stats for cache: {}", name, ex);
                return null;
            });
        });
    }
}
