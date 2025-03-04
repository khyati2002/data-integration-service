package com.applicate.services.channelkart.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class EntityCacheManager {
    private static final EntityCacheManager INSTANCE = new EntityCacheManager();
    
    private final Map<Class<?>, Map<Object, Object>> cache = new ConcurrentHashMap<>();

    private EntityCacheManager() {}

    public static EntityCacheManager getInstance() {
        return INSTANCE;
    }

    public Map<Object, Object> getEntityCache(Class<?> entityClass) {
        return cache.computeIfAbsent(entityClass, k -> new ConcurrentHashMap<>());
    }

    public void put(Class<?> entityClass, Object key, Object value) {
        getEntityCache(entityClass).put(key, value);
    }

    public Object get(Class<?> entityClass, Object key) {
        return getEntityCache(entityClass).get(key);
    }
}
