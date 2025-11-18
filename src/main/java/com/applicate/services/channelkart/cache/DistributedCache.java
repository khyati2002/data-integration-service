package com.applicate.services.channelkart.cache;

import com.applicate.services.channelkart.abstractdatasource.AbstractDataSourceConstants;
import com.applicate.services.channelkart.client.properties.PropertyDefinition;
import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import com.applicate.services.channelkart.utils.GlobalLock;
import com.applicate.services.channelkart.utils.SecurityContextUtils;;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.client.codec.Codec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DistributedCache {

    private static final Logger logger = LoggerFactory.getLogger(DistributedCache.class);
    private static DistributedCache INSTANCE;
    private static final String UPDATE_PUBSUB_TOPIC = "change-publisher";
    private Map<String, RMap<String, Object>> cmc = new ConcurrentHashMap<>();

    @Getter
    private static RedissonClient redissonClient;
    private static final String DEFAULT_CACHE_NAME = AbstractDataSourceConstants.DEFAULT;
    @Getter
    @Setter
    private RedissonClient redisson;
    private static String cacheStore = "datastore";
    @Setter
    private PropertyRegistry propertyRegistry;
    boolean isFistLevelCacheEnabled;
    @Setter
    private String environment;
    private final Map<String, Consumer<CacheUpdateEvent>> changeEventSubscribers = new HashMap<>();
    private boolean localCacheMap = false;
    private static final int MAX_CACHE_MAP_SIZE = 5000;

    private DistributedCache(Properties properties) {
        String redisUrl = properties.getProperty("redisUrl");
        boolean clustered = Boolean.parseBoolean(properties.getProperty("cacheClustered", "false"));
        int subscriptionConnectionPoolSize= Integer.parseInt(properties.getProperty("subscriptionConnectionPoolSize", "8"));
        int subscriptionsPerConnection= Integer.parseInt(properties.getProperty("subscriptionsPerConnection", "4"));
        int masterConnectionPoolSize= Integer.parseInt(properties.getProperty("masterConnectionPoolSize", "8"));
        int slaveConnectionPoolSize= Integer.parseInt(properties.getProperty("slaveConnectionPoolSize", "4"));
        int idleConnectionTimeout= Integer.parseInt(properties.getProperty("idleConnectionTimeout", "180000"));
        int idleMasterConnectionPoolSize= Integer.parseInt(properties.getProperty("idleMasterConnectionPoolSize", "4"));
        int idleSlaveConnectionPoolSize= Integer.parseInt(properties.getProperty("idleSlaveConnectionPoolSize", "2"));
        if (StringUtils.isNotBlank(redisUrl)) {
            Config config = new Config().setCodec(getCodec());
            if (clustered) {
                config.useClusterServers().setTimeout(30000)
                        .setRetryAttempts(5)
                        .setMasterConnectionPoolSize(masterConnectionPoolSize)
                        .setSlaveConnectionPoolSize(slaveConnectionPoolSize)
                        .setIdleConnectionTimeout(idleConnectionTimeout)
                        .setMasterConnectionMinimumIdleSize(idleMasterConnectionPoolSize)
                        .setSlaveConnectionMinimumIdleSize(idleSlaveConnectionPoolSize)
                        .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
                        .setSubscriptionsPerConnection(subscriptionsPerConnection)
                        .addNodeAddress(redisUrl);
            } else {
                config.useSingleServer().
                        setTimeout(30000)
                        .setRetryAttempts(5)
                        .setConnectionPoolSize(masterConnectionPoolSize)
                        .setConnectionMinimumIdleSize(idleMasterConnectionPoolSize)
                        .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
                        .setSubscriptionsPerConnection(subscriptionsPerConnection)
                        .setAddress(redisUrl);
            }
            redissonClient = Redisson.create(config);

            subscribeForChangeEvents();
        }
    }

    public static DistributedCache getInstance(Properties properties) {
        if(INSTANCE == null){
            INSTANCE = new DistributedCache(properties);
        }
      INSTANCE.setRedisson(redissonClient);
        return INSTANCE;
    }
    public static DistributedCache getInstance() {
        if(INSTANCE == null){
            throw new RuntimeException("DistributedCache not initialized. Call getInstance(Properties properties) first.");
        }
        return INSTANCE;
    }


    private Codec getCodec() {
        return new ByteArrayCodec() {
            private final Encoder encoder = in -> {
                if (in instanceof byte[]) {
                    return Unpooled.wrappedBuffer((byte[]) in);
                } else {
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

    private void subscribeForChangeEvents() {
        if (redisson != null) {
            RTopic topic = redisson.getTopic(env() + "-" + UPDATE_PUBSUB_TOPIC);
            topic.addListener(CacheUpdateEvent.class, (charSequence, event) -> {
                logger.debug("Cache update Event {}, Domain ->{}, Key->{}", event.getLob(), event.getDomainName(), event.getKey());
                try {
                    if (event instanceof LOBRegisterEvent) {
                        logger.warn("LOBRegisterEvent received but StartupBooster not available in non-Spring context");
                    } else if (event instanceof AppCacheRemoveEvent) {
                        logger.info("Removing AppCacheManager cache for lob:{}", event.getLob());
                        AppCacheManager.getInstance().removeAll(event.getLob());
                    } else {
                        clearCacheOnEvent(event);
                    }
                } catch (Exception e) {
                    logger.error("Could not listener cache change event", e);
                }
            });
        }
    }

    private void clearCacheOnEvent(CacheUpdateEvent event) {
            if (event.getKey() != null) {
                AppCacheManager.getInstance()
                        .removeByDomain(event.getDomainName(), event.getKey());
            } else {
                AppCacheManager.getInstance()
                        .removeByDomain(event.getDomainName());
            }
            var cacheUpdateEventConsumer = changeEventSubscribers.get(event.getDomainName());
            if (cacheUpdateEventConsumer != null) {
                cacheUpdateEventConsumer.accept(event);
            }
            logger.debug("Cleared first level cache for key {}", event.getKey());
    }

      
    public void publishChangeEvent(CacheUpdateEvent e) {
        if (redisson != null) {
            RTopic topic = redisson.getTopic(env() + "-" + UPDATE_PUBSUB_TOPIC);
            long clientsReceivedMessage = topic.publish(e);
            logger.debug("Cache published event ->{} for event:{}", clientsReceivedMessage, e);
        }
    }

    private void logCached(String cacheName, Object item) {
        logger.debug("cached {} {}", cacheName, item);
    }

    public void put(String lob, String domainName, Map<String, Object> dataMap) {
        String cacheName = getCacheName(lob, domainName);
        if (redisson != null) {
            try {
                RMap<String, Object> map = getMap(cacheName);
                map.putAll(dataMap);
            } catch (Exception e) {
                logger.error("Distributed cache put for key {} failed with error:", cacheName, e);
            }
        }
    }

    public void put(String lob, String domainName, String key, Object obj, boolean isRaw) {
        put(lob, domainName, key, obj, isRaw, false);
    }


    public void put(String lob, String domainName, String key, Object obj, boolean isRaw, boolean isFistLevelCacheEnabled) {
        String cacheName = getCacheName(lob, domainName);
        if (redisson != null) {
            try {
                RMap<String, Object> map = getMap(cacheName);
                logCached(cacheName, obj);
                if (map.size() > MAX_CACHE_MAP_SIZE) {
                    map.clear();
                }
                GlobalLock.withLock(key, k -> map.fastPut(k, obj));
                if ((isFistLevelCacheEnabled || this.isFistLevelCacheEnabled) && lob != null) {
                    AppCacheManager.getInstance().put(cacheName, key, obj);
                }
            } catch (Exception e) {
                logPutError(cacheName, key, obj, e);
            }
        } else {
            AppCacheManager.getInstance().put(cacheName, key, obj);
        }
    }

    public void put(String lob, String domainName, String key, Object obj, long ttl, TimeUnit timeUnit, boolean localCacheMap) {
        String cacheName = getCacheName(lob, domainName);
        if (redisson != null) {
            try {
                RMap<String, Object> map = getMap(cacheName, localCacheMap);
                logCached(cacheName, obj);
                if (map.size() > MAX_CACHE_MAP_SIZE) {
                    map.clear();
                }
                if (map instanceof RMapCache) {
                    ((RMapCache) map).fastPut(key, obj, ttl, timeUnit);
                } else {
                    map.fastPut(key, obj);
                }

            } catch (Exception e) {
                logPutError(cacheName, key, obj, e);
            }
        } else {
            AppCacheManager.getInstance().put(cacheName, key, obj);
        }
    }

      
    public void putAllBatch(String lob, String domainName, Map<String, Object> entries, long ttl, TimeUnit timeUnit, boolean localCacheMap) {
        String cacheName = getCacheName(lob, domainName);
        if (redisson != null) {
            try {
                RMap<String, Object> map = getMap(cacheName, localCacheMap);
                logCached(cacheName, entries);

                if (map.size() > MAX_CACHE_MAP_SIZE) {
                    map.clear();
                }

                RBatch batch = redisson.createBatch();

                if (localCacheMap) {
                    RMapCacheAsync<String, Object> asyncMap = batch.getMapCache(cacheName);
                    entries.forEach((key, value) ->
                                            asyncMap.fastPutAsync(key, value, ttl, timeUnit)
                    );
                } else {
                    RMapAsync<String, Object> asyncMap = batch.getMap(cacheName);
                    entries.forEach(asyncMap::fastPutAsync);
                }

                batch.executeAsync();

            } catch (Exception e) {
                logPutAllError(cacheName, entries, e);
            }
        } else {
            entries.forEach((key, value) ->
                                    AppCacheManager.getInstance().put(cacheName, key, value)
            );
        }
    }

    private void logPutAllError(String cacheName, Map<String, Object> entries, Exception e) {
        entries.forEach((key, value) -> logPutError(cacheName, key, value, e));
    }

    private void logPutError(String cacheName, String key, Object object, Exception e) {
        logger.error("Could not put the object for cacheName:{}, key:{}, item:{}", cacheName, key, object, e);
    }

    private void logGetError(String cacheName, String key, Exception e) {
        logger.error("Could not get object from cacheName:{}, key:{}", cacheName, key, e);
    }

      
    public void removeStream(String lob, String domainName) {
        String cacheName = getCacheName(lob, domainName);
        if (redisson != null) {
            try {
                RMap<String, Object> map = getMap(cacheName);
                map.keySet().removeIf(e -> domainName.equalsIgnoreCase(((StoreKey) map.get(e)).getDomain()));
            } catch (Exception e) {
                logger.error("Could not remove stream for cacheName:{}", cacheName, e);
            }
        }
    }

    public Map<String, Object> get(String lob, String domainName, Collection<String> keys) {
        String cacheName = getCacheName(lob, domainName);
        if (redisson != null) {
            try {
                Map<String, Object> objects = new HashMap<>();
                RMap<String, Object> map = getMap(cacheName);
                keys.forEach(key -> {
                    Object v = map.get(key);
                    if (v != null) {
                        objects.put(key, v);
                    }
                });
                return objects;
            } catch (Exception e) {
                logger.error("Could  not get cache value for key:{} ", cacheName, e);
                return null;
            }
        } else {
            return null;
        }
    }

    public Object get(String lob, String domainName, String key, boolean isRaw) {
        return get(lob, domainName, key, isRaw, false, localCacheMap);
    }

    private String makeGlobalDomainKey(String domainName, String key) {
        return domainName + ":" + key;
    }

      
    @SuppressWarnings("unchecked")
    public <T> T getValueByKey(String domainName, String key, Supplier<T> valueSupplier) {
        String lookupKey = makeGlobalDomainKey(domainName, key);
        Object value = redisson.getBucket(lookupKey).get();
        if (value == null) {
            value = valueSupplier.get();
            if (value != null) {
                redisson.getBucket(lookupKey).set(value);
            }
        }
        return (T) value;
    }

      
    @SuppressWarnings("unchecked")
    public <T> T getValueByKey(String domainName, String key) {
        String lookupKey = makeGlobalDomainKey(domainName, key);
        return (T) redisson.getBucket(lookupKey).get();
    }

      
    public <T> void putValueByKey(String domainName, Map<String, T> keyValuePairs) {
        keyValuePairs.forEach((key, value) -> {
            String keyName = makeGlobalDomainKey(domainName, key);
            redisson.getBucket(keyName).set(value);
        });
    }

      
    public Object get(String lob, String domainName, String key, boolean isRaw, boolean isFistLevelCacheEnabled, boolean localCacheMap) {
        String cacheName = getCacheName(lob, domainName);
        if (redisson != null) {
            try {
                Object object = null;
                if ((isFistLevelCacheEnabled || this.isFistLevelCacheEnabled) && lob != null && key != null) {
                    object = AppCacheManager.getInstance().get(cacheName, key);
                }
                if (object == null) {
                    RMap<String, Object> map = getMap(cacheName, localCacheMap);
                    object = map.get(key);
                }
                return object;
            } catch (Exception e) {
                logGetError(cacheName, key, e);
                return null;
            }
        } else {
            return AppCacheManager.getInstance().get(cacheName, key);
        }
    }

    private void logError(String cacheName, Exception e) {
        logger.error("Exception happened while accessing the cacheName:{}", cacheName, e);
    }

      
    public Map<String, Object> get(String lob, String domainName) {
        String cacheName = getCacheName(lob, domainName);
        HashMap<String, Object> dataMap = new HashMap<>();
        if (redisson != null) {
            try {
                RMap<String, Object> map = getMap(cacheName);
                map.keySet().forEach(k -> dataMap.put(k, map.get(k)));
            } catch (Exception e) {
                logError(cacheName, e);
                return null;
            }
        }
        return dataMap;
    }

    public Map<String, Object> resolveAndGet(String cacheName) {
        return get(getCacheName(cacheName));
    }

      
    public Map<String, Object> get(String cacheName) {
        HashMap<String, Object> dataMap = new HashMap<>();
        if (redisson != null) {
            try {
                RMap<String, Object> map = getMap(cacheName);
                map.keySet().forEach(k -> dataMap.put(k, map.get(k)));
            } catch (Exception e) {
                logError(cacheName, e);
                return null;
            }
        }
        return dataMap;
    }

      
    public void clearCache(String lob, String domainName, String key) {
        clearCache(lob, domainName, key, localCacheMap);
    }

      
    public void clearCache(String lob, String domainName, String key, boolean localCacheMap) {
        String cacheName = getCacheName(lob, domainName);
        if (redisson != null) {
            try {
                RMap<String, Object> map = getMap(cacheName, localCacheMap);
                if (map.containsKey(key)) {
                    map.remove(key);
                    AppCacheManager.getInstance().remove(cacheName, key);
                    sendCacheUpdateEvent(domainName, lob, key);
                }
            } catch (Exception e) {
                logError(cacheName, e);
            }
        } else {
            AppCacheManager.getInstance().remove(cacheName, key);
        }
    }

    private void sendCacheUpdateEvent(String domainName, String lob, String key) {
        try {
            CacheUpdateEvent cue = new CacheUpdateEvent();
            cue.setDomainName(domainName);
            cue.setLob(lob);
            cue.setKey(key);
        } catch (Exception e) {
            logger.error("Could not send cache update event for domainName:{}, key:{}", domainName, key, e);
        }
    }

      
    public void clearCache(String lob, String domainName) {
        String cacheName = getCacheName(lob, domainName);
        if (redisson != null) {
            try {
                RMap<String, Object> map = getMap(cacheName);
                map.clear();
            } catch (Exception e) {
                logError(cacheName, e);
            }
        } else {
            AppCacheManager.getInstance().clearCache(lob, cacheName);
        }
    }

      
    @SuppressWarnings("unchecked")
    public <V> V withCache(String lob, String key, Function<String, V> function) {
        V cached = (V) get(lob, null, key, false);
        if (cached == null) {
            cached = function.apply(key);
            if (cached != null) {
                put(lob, null, key, cached, false);
            }
        }
        return cached;
    }

      
    @SuppressWarnings("unchecked")
    public <V> V withCache(String lob,String domain,String key, Function<String, V> function) {
        V cached = (V) get(lob, domain, key, false);
        if (cached == null) {
            cached = function.apply(key);
            if (cached != null) {
                put(lob, domain, key, cached, false);
            }
        }
        return cached;
    }

      
    @SuppressWarnings("unchecked")
    public <V> V withExpiringCache(String lob, String domain, String key, long ttl, TimeUnit unit, Function<String, V> function) {
        V cached = (V) get(lob, domain, key, false, false, false);
        if (cached == null) {
            cached =  function.apply(key);
            if (cached != null) {
                put(lob, domain, key, cached, ttl, unit, false);
            }
        }
        return cached;
    }

    private boolean isRoot() {
        String lob = SecurityContextUtils.getLob();
        return lob == null || ("default".equalsIgnoreCase(lob) || "root".equalsIgnoreCase(lob));
    }

      
    public List<String> getAllKeys() {
        List<String> keyList = new ArrayList<>();
        if (redisson != null) {
            RKeys keys = redisson.getKeys();
            boolean isRoot = isRoot();
            String lob = isRoot ? null : SecurityContextUtils.getLob().toLowerCase();
            keys.getKeys().forEach(k -> {
                if (isRoot) {
                    if (k.toLowerCase().startsWith(env())) {
                        keyList.add(k);
                    }
                } else {
                    if (k.toLowerCase().contains(lob)) {
                        keyList.add(k);
                    }
                }
            });
        }
        return keyList;
    }

      
    public List<StoreKey> getStreams(String lob) {
        String cacheName = getCacheName(lob, cacheStore);
        List<StoreKey> storeKeys = new ArrayList<>();
        if (redisson != null) {
            try {
                RMap<String, Object> map = getMap(cacheName);
                map.keySet().forEach(k -> storeKeys.add((StoreKey) map.get(k)));
            } catch (Exception e) {
                logError(cacheName, e);
            }
        }
        return storeKeys;
    }

      
    public void deleteKey(List<String> keys) {
        if (redisson != null) {
            redisson.getKeys().delete(keys.toArray(new String[0]));
        }
        keys.forEach(key -> {
            RMap<String, Object> map = cmc.remove(key);
            destroyIfLocalAndRemoveFromCmc(map);
        });
    }

      
    public void deleteRedisCacheMap(List<String> keys) {
        if (keys.isEmpty()) {
            keys.addAll(getAllKeys());
        }
        keys.forEach(key -> {
            RMap<String, Object> map = cmc.remove(key);
            destroyIfLocalAndRemoveFromCmc(map);
        });
    }

    private String envName(String cacheName) {
        return env() + ":" + cacheName;
    }

    private String env() {
        if (environment == null) {
            return "dev";
        }
        return environment;
    }

    private <T> RMap<String, T> getMap(String cacheName) {
        return getMap(cacheName, localCacheMap);
    }

    @SuppressWarnings("unchecked")
    private <T> RMap<String, T> getMap(String cacheName, boolean useLocalCacheMap) {
        return (RMap<String, T>) cmc.computeIfAbsent(cacheName, k -> {
            if (useLocalCacheMap) {
                return redisson.getLocalCachedMap(k, LocalCachedMapOptions.defaults());
            } else {
                return redisson.getMapCache(k);
            }
        });
    }

    public void deleteKeys(String keyToDelete) {
        try {
            if (redisson != null) {
                List<String> allKeys = getAllKeys();
                List<String> keysToDelete = allKeys.stream()
                                                    .filter(key -> key.equals(getCacheName(SecurityContextUtils.getLob(), keyToDelete)))
                                                    .collect(Collectors.toList());
                if (!keysToDelete.isEmpty()) {
                    redisson.getKeys().delete(keysToDelete.toArray(new String[0]));
                }
                keysToDelete.forEach(key -> {
                    RMap<String, Object> map = cmc.remove(key);
                    destroyIfLocalAndRemoveFromCmc(map);
                });
            }
        } catch (Exception e) {
            logger.error("Could not clear distributed cache");
        }
    }

      
    public boolean deleteAll() {
        try {
            if (redisson != null) {
                List<String> allKeys = getAllKeys();
                List<String> cacheDomainToIgnore = propertyRegistry != null ?
                                                           propertyRegistry.getAsList(PropertyDefinition.IGNORE_CACHE_DOMAIN_WHILE_CLEARING) :
                                                           new ArrayList<>();
                List<String> keysToDelete = allKeys.stream()
                                                    .filter(key -> !cacheDomainToIgnore.contains(key))
                                                    .collect(Collectors.toList());

                if (!keysToDelete.isEmpty()) {
                    redisson.getKeys().delete(keysToDelete.toArray(new String[0]));
                }
                keysToDelete.forEach(key -> {
                    RMap<String, Object> map = cmc.remove(key);
                    destroyIfLocalAndRemoveFromCmc(map);
                });
            }
            return true;
        } catch (Exception e) {
            logger.error("Could not clear distributed cache");
            return false;
        }
    }

      
    public String getCacheName(String lob, String domainName) {
        String cacheName = isDefaultDataSource(lob) ? DEFAULT_CACHE_NAME : lob;
        cacheName = domainName == null ? cacheName : cacheName + ":" + domainName;
        cacheName = envName(cacheName);
        return cacheName;
    }

      
    public String getCacheName(String domainName) {
        String lob = SecurityContextUtils.getLob();
        return getCacheName(lob, domainName);
    }

      
    public void publishCacheEvent(String lob, String key, CacheOperationsConstant operation, Object data) {
        AppCacheEvent.Builder builder = new AppCacheEvent.Builder(operation, null)
                                                .setKey(key)
                                                .setLob(lob)
                                                .setData(data);
        AppCacheEventPublisher.get().publishEvent(builder, () -> null);
    }

      
    public void count(String name) {
        if (redisson != null) {
            String key = env() + ":" + SecurityContextUtils.getLob() + ":" + name;
            redisson.getAtomicLong(key).incrementAndGet();
        }
    }

      
    public Long getCount(String name) {
        if (redisson != null) {
            String key = env() + ":" + SecurityContextUtils.getLob() + ":" + name;
            return redisson.getAtomicLong(key).get();
        }
        return 0L;
    }

      
    public Long getCount(String lob, String name) {
        if (redisson != null) {
            String key = env() + ":" + lob + ":" + name;
            return redisson.getAtomicLong(key).get();
        }
        return 0L;
    }

      
    public void addChangeEventListener(String domain, Consumer<CacheUpdateEvent> consumer) {
        changeEventSubscribers.put(domain, consumer);
    }

      
    public <T> void putKeyValue(String key, T value) {
        redisson.getBucket(key).set(value);
    }

      
    public boolean removeByKey(String key) {
        return redisson.getBucket(key).delete();
    }

    @SuppressWarnings("unchecked")
      
    public <T> T getValue(String key) {
        return (T) redisson.getBucket(key).get();
    }

      
    public <T> T withLock(String lockable, Supplier<T> supplier) {
        RLock lock = redisson.getLock(lockable);
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    private void destroyIfLocalAndRemoveFromCmc(RMap<String, ?> map) {
        if (map == null) return;
        if (map instanceof RLocalCachedMap) {
            ((RLocalCachedMap<?, ?>) map).destroy();    // tears down PubSub subscription
        } else {
            map.clear(); // fallback for non-local maps
        }
    }

    public  boolean isDefaultDataSource(String lob) {
        return (lob == null || lob.equals(AbstractDataSourceConstants.DEFAULT));
    }

    public void getAllCachesAndClear(String prefix, String suffix) {
        try {
            if (redisson != null) {
                Iterable<String> keys = redisson.getKeys().getKeysByPattern(prefix + "*" + suffix);

                for (String key : keys) {
                    RMapCache<String, Object> cache = redisson.getMapCache(key);
                    cache.clear();
                    cmc.remove(key); // Remove from local map

                    logger.info("Cleared cache: {}", key);
                }

                logger.info("Successfully cleared all caches with prefix: {} and suffix: {}", prefix, suffix);
            }
        } catch (Exception e) {
            logger.error("Error clearing caches with prefix {} and suffix {}", prefix, suffix, e);
        }
    }

      
    public RMapCache<String, Object> getCache(String name, int expireAfterMinutes) {
        return (RMapCache<String, Object>) cmc.computeIfAbsent(name, n -> {
            if (redisson != null) {
                return redisson.getMapCache(n);
            }
            return null;
        });
    }

      
    public void evictAll(String cacheName) {
        cacheName = getCacheName(cacheName);
        if (redisson != null) {
            try {
                RMapCache<String, Object> cache = redisson.getMapCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    logger.info("Evicted all entries from Redis cache: {}", cacheName);
                }
                cmc.remove(cacheName);
                String lob = SecurityContextUtils.getLob();
                AppCacheManager.getInstance().clearCache(lob, cacheName);

            } catch (Exception e) {
                logger.error("Error evicting all entries from cache: {}", cacheName, e);
            }
        } else {
            String lob = SecurityContextUtils.getLob();
            AppCacheManager.getInstance().clearCache(lob, cacheName);
        }
    }


}