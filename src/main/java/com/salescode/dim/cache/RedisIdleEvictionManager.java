package com.salescode.dim.cache;


import com.applicate.services.channelkart.cache.DistributedCache;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class RedisIdleEvictionManager {

    private static final Logger logger = LoggerFactory.getLogger(RedisIdleEvictionManager.class);
    private static final String LOCK_PREFIX = "fileIdLock:";
    private static RedisIdleEvictionManager redisIdleEvictionManager;
    private final RedissonClient redisson = DistributedCache.getRedissonClient();

    public static RedisIdleEvictionManager getInstance() {
        if (redisIdleEvictionManager == null) {
            redisIdleEvictionManager = new RedisIdleEvictionManager();
        }
        return redisIdleEvictionManager;
    }

    /**
     * Generate or retrieve a cached fileId with atomicity across distributed systems
     *
     * @param lob        Line of business
     * @param masterName Domain identifier
     * @param key        The cache key
     * @param ttl        Time to live
     * @param timeUnit   Time unit for TTL
     * @return The unique fileId that's consistent across all service instances
     */
    public String getOrCreateFileId(String lob, String masterName, String key, long ttl, TimeUnit timeUnit) {
        String cacheName = getCacheName(lob, masterName);
        String lockKey = LOCK_PREFIX + cacheName + ":" + key;

        // First try to get without locking for performance
        RMapCache<String, String> mapCache = redisson.getMapCache(cacheName);
        String existingValue = mapCache.get(key);
        if (existingValue != null) {
            return existingValue;
        }

        // Need to create value - use distributed lock to ensure atomicity
        RLock lock = null;
        try {
            lock = redisson.getLock(lockKey);
            // Wait up to 5 seconds for lock
            if (lock.tryLock(5, TimeUnit.SECONDS)) {
                // Double-check after acquiring lock
                existingValue = mapCache.get(key);
                if (existingValue != null) {
                    return existingValue;
                }
                // Generate new value and store it
                String newFileId = UUID.randomUUID().toString();
                mapCache.put(key, newFileId, 0, timeUnit, ttl, timeUnit);
                return newFileId;
            } else {
                // Couldn't get lock in time - try one more read
                existingValue = mapCache.get(key);
                if (existingValue != null) {
                    return existingValue;
                }

                // Last resort fallback - generate local value
                logger.warn("Failed to acquire distributed lock for fileId generation: {}", lockKey);
                return UUID.randomUUID().toString();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting for fileId lock: {}", lockKey, e);
            return UUID.randomUUID().toString();
        } catch (Exception e) {
            logger.error("Error in distributed fileId generation: {}", e.getMessage(), e);
            return UUID.randomUUID().toString();
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    private String getCacheName(String lob, String domainName) {
        String cacheName = lob;
        if (domainName != null) {
            cacheName = cacheName + ":" + domainName;
        }
        return cacheName;
    }
}