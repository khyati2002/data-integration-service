package com.salescode.dim.cache;



import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.client.codec.Codec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;
import org.redisson.config.Config;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class CacheManager {
    private static CacheManager INSTANCE;
    private static RedissonClient redissonClient;
    private final Map<String, RMapCache<String, Object>> caches = new ConcurrentHashMap<>();


    private Codec getCodec(){
        return new ByteArrayCodec(){
            private final Encoder encoder = in -> {
                if(in instanceof byte[]) {
                    return Unpooled.wrappedBuffer((byte[]) in);
                }else{
                    return Unpooled.wrappedBuffer(SerializationUtils.serialize(((Serializable) in)));
                }
            };

            private final Decoder<Object> decoder = (buf, state) -> {
                byte[] result = new byte[buf.readableBytes()];
                buf.readBytes(result);

                return SerializationUtils.deserialize(result);
            };

            @Override
            public Encoder getValueEncoder() {
                return encoder;
            }



            @Override
            public Decoder<Object> getValueDecoder() {
                return decoder;
            }

        };
    }

    private CacheManager(Properties properties) {
        // Initialize Redisson Client
        String redisUrl = properties.getProperty("redisUrl");
        boolean clustered = Boolean.parseBoolean(properties.getProperty("cacheClustered", "false"));
        int subscriptionConnectionPoolSize= Integer.parseInt(properties.getProperty("subscriptionConnectionPoolSize", "250"));
        int subscriptionsPerConnection= Integer.parseInt(properties.getProperty("subscriptionsPerConnection", "25"));

        if (StringUtils.isNotBlank(redisUrl)) {
            Config config = new Config().setCodec(getCodec());
            if (clustered) {
                config.useClusterServers().setTimeout(30000)
                        .setRetryAttempts(5)
                        .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
                        .setSubscriptionsPerConnection(subscriptionsPerConnection)
                        .addNodeAddress(redisUrl);
            } else {
                config.useSingleServer().
                        setTimeout(30000)
                        .setRetryAttempts(5)
                        .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
                        .setSubscriptionsPerConnection(subscriptionsPerConnection)
                        .setAddress(redisUrl);
            }
            redissonClient = Redisson.create(config);
        }
    }

    public static CacheManager getInstance(Properties properties) {
        if(INSTANCE == null){
            INSTANCE = new CacheManager(properties);
        }
        return INSTANCE;
    }

    public static CacheManager getInstance() {
        if(INSTANCE == null){
            throw new RuntimeException("CacheManager not initialized. Call getInstance(Properties properties) first.");
        }
        return INSTANCE;
    }


    /**
     * Get or create a Redis-backed cache with TTL.
     */
    public RMapCache<String, Object> getCache(String name, int expireAfterMinutes) {
        return caches.computeIfAbsent(name, n -> {
            RMapCache<String, Object> mapCache = redissonClient.getMapCache(n);
            return mapCache;
        });
    }

    /**
     * Add an entry to the cache with expiration time.
     */
    public void put(String cacheName, String key, Object value, int expireAfterMinutes) {
        RMapCache<String, Object> cache = getCache(cacheName, expireAfterMinutes);
        cache.put(key, value, expireAfterMinutes, TimeUnit.MINUTES);
    }

    /**
     * Get an entry from the cache.
     */
    public Object get(String cacheName, String key) {
        RMapCache<String, Object> cache = caches.get(cacheName);
        return (cache != null) ? cache.get(key) : null;
    }

    /**
     * Remove all entries from a specific cache.
     */
    public void evictAll(String cacheName) {
        RMapCache<String, Object> cache = caches.get(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Remove cache entries by matching keys with a regex pattern.
     */
    public void evictByPattern(String cacheName, String keyPattern) {
        RMapCache<String, Object> cache = caches.get(cacheName);
        if (cache != null && keyPattern != null && !keyPattern.isEmpty()) {
            Pattern pattern = Pattern.compile(keyPattern);
            cache.keySet().stream()
                    .filter(key -> pattern.matcher(key).matches())
                    .forEach(cache::remove);
        }
    }

    public void clearCachePattern(Pattern pattern) {
        caches.forEach((cacheName, cache) -> {
            Set<String> keys = cache.keySet();
            keys.stream()
                    .filter(key -> pattern.matcher(key).matches())
                    .forEach(cache::remove);
        });
    }

    /**
     * Shutdown Redis client gracefully.
     */
    public void shutdown() {
        redissonClient.shutdown();
    }}
