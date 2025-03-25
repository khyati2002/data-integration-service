// 3. CacheManager to handle multiple caches
package com.salescode.dim.cache;

import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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

//    public void printStats() {
//        caches.forEach((name, cache) -> {
//            if (cache.stats() != null) {
//                System.out.println("Cache: " + name);
//                System.out.println("  Hit rate: " + cache.stats().hitRate());
//                System.out.println("  Miss rate: " + cache.stats().missRate());
//                System.out.println("  Request count: " + cache.stats().requestCount());
//                System.out.println("  Size: " + cache.estimatedSize());
//            }
//        });
//    }
}
