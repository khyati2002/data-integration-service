package com.salescode.channelkart.cache;


import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.utils.NullUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class AppCacheManager {

    private static final Logger log = LoggerFactory.getLogger(AppCacheManager.class);

    private static final String COMMON_CACHE_NAME = "commonCache";
    private final Map<String, Cache<String, Object>> metaCache = new ConcurrentHashMap<>();
    private static final AppCacheManager INSTANCE = new AppCacheManager();
    private static final String CACHE_PROVIDER = "org.ehcache.jsr107.EhcacheCachingProvider";
    private static final Object NULL_OBJECT = new Object();


    private AppCacheManager() {
    }

    public static AppCacheManager getInstance() {
        return INSTANCE;
    }


    public Object get(String key) {
        return get(null, key);
    }

    public Object get(String lob, String key) {
        String cacheName = lob != null ? lob : COMMON_CACHE_NAME;
        Cache<String, Object> cache = metaCache.get(cacheName);
        return cache != null ? cache.get(key) : null;
    }


    public <T> T get(String lob, String key, Class<T> type) {
        String cacheName = lob != null ? lob : COMMON_CACHE_NAME;
        Cache<String, Object> cache = metaCache.get(cacheName);
        return cache != null ? type.cast(cache.get(key)) : null;
    }

    public void put(String key, Object value) {
//        put(null, key, value);
    }

    public void put(String lob, String key, Object value) {
//        String cacheName = lob != null ? lob : COMMON_CACHE_NAME;
//        Cache<String, Object> cache = ensureCache(cacheName);
//        cache.put(key, value);
    }

    public <V> V withCache(String cacheDomain, String key, Function<String, V> function) {
        String lob = SecurityContextUtils.getLob();
        String cacheName = lob != null ? lob : COMMON_CACHE_NAME;
        String fullKey = cacheDomain + ":" + key;
        V cached = (V) get(cacheName, fullKey);
        if (cached == NULL_OBJECT) {
            return null;
        } else if (cached == null) {
            cached = function.apply(key);
            V storeData = cached;
            if (storeData == null) {
                storeData = (V) NULL_OBJECT;
            }
            put(cacheName, fullKey, storeData);
        }
        return cached;
    }

    private Cache<String, Object> ensureCache(String name) {
        Cache<String, Object> cache = metaCache.get(name);
        if (cache == null) {
            synchronized (this) {
                cache = metaCache.get(name);
                if (cache == null) {
                    cache = createCache(name);
                    metaCache.put(name, cache);
                }
            }

        }
        return cache;
    }

    private synchronized Cache<String, Object> createCache(String name) {

        CachingProvider provider = Caching.getCachingProvider(CACHE_PROVIDER);
        CacheManager cacheManager = provider.getCacheManager();
        MutableConfiguration<String, Object> configuration = new MutableConfiguration<String, Object>()
                .setTypes(String.class, Object.class).setStoreByValue(false)
                .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));
        Cache<String, Object>  cache = cacheManager.getCache(name);
        if(cache==null){
            cache = cacheManager.createCache(name, configuration);
        }
        return cache;
        
    }

    public boolean remove(String key) {
        String lob = SecurityContextUtils.getLob();
        String cacheName = lob != null ? lob : COMMON_CACHE_NAME;
        Cache<String, Object> cache = metaCache.get(cacheName);
        if (NullUtils.isNotNull(cache) && cache.containsKey(key)) {
            return cache.remove(key);
        }
        return true;
    }

    public boolean removeByDomain(String cacheDomain, String key) {
        String fullKey = cacheDomain + ":" + key;
        return remove(fullKey);
    }

    public void removeByDomain(String cacheDomain) {
        String lob = SecurityContextUtils.getLob();
        clearCache(lob, cacheDomain);
    }

    public void clearCache(String lob, String cacheDomain) {
        String cacheName = lob != null ? lob : COMMON_CACHE_NAME;
        Cache<String, Object> cache = metaCache.get(cacheName);
        if (NullUtils.isNotNull(cache)) {
            log.info("removing app cache for lob:{}, domain:{}", lob, cacheDomain);
            cache.removeAll();
        }
    }

    public boolean remove(String lob, String key) {
        String cacheName = lob != null ? lob : COMMON_CACHE_NAME;
        Cache<String, Object> cache = metaCache.get(cacheName);
        if (NullUtils.isNotNull(cache) && cache.containsKey(key)) {
            return cache.remove(key);
        }
        return true;
    }

    private boolean isRoot(){
        String lob=SecurityContextUtils.getLob();
        return lob == null || ("default".equalsIgnoreCase(lob) || "root".equalsIgnoreCase(lob));
    }

    public void clearCache(String lob, String domainName, String key) {
        if (key == null) {
            removeByDomain(domainName);
        } else {
            removeByDomain(domainName, key);
        }
        //publishOnCacheChange(lob,domainName,key);
    }

    public static void main(String[] args) {
        AppCacheManager ac = AppCacheManager.getInstance();
        ac.put("test", "1", "one");
        String value = ac.get("test", "1").toString();
        log.info(value);
    }

}
