///*
// * Copyright (c) 2020. All rights reserved.
// * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
// *
// */
//package com.salescode.dataintegration.etl.cdm.cache;
//
//import com.salescode.channelkart.abstractdatasource.AbstractDataSourceConstants;
//import com.salescode.channelkart.storage.MediaOperationServiceProvider;
//import io.netty.buffer.Unpooled;
//import io.opentelemetry.instrumentation.annotations.SpanAttribute;
//import io.opentelemetry.instrumentation.annotations.WithSpan;
//import org.apache.commons.io.IOUtils;
//import org.apache.commons.lang.SerializationUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.redisson.Redisson;
//import org.redisson.api.*;
//import org.redisson.client.codec.ByteArrayCodec;
//import org.redisson.client.codec.Codec;
//import org.redisson.client.protocol.Decoder;
//import org.redisson.client.protocol.Encoder;
//import org.redisson.config.Config;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.env.Environment;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.PostConstruct;
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.Serializable;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.TimeUnit;
//import java.util.function.Consumer;
//import java.util.function.Function;
//import java.util.function.Supplier;
//
//@Service
//public class DistributedCache {
//
//   private static final Logger logger = LoggerFactory.getLogger(DistributedCache.class);
//
//   private static final String UPDATE_PUBSUB_TOPIC ="change-publisher";
//
//   private Map<String, RMap<String, Object>> cmc=new ConcurrentHashMap<>();
//
//
//   @Autowired
//   private Environment env;
//
//   private static final String DEFAULT_CACHE_NAME = AbstractDataSourceConstants.DEFAULT;
//
//   private RedissonClient redisson;
//
//   private static String cacheStore = "datastore";
//
//   @Autowired
//   private MediaOperationServiceProvider mediaServiceProvider;
//
//   boolean isFistLevelCacheEnabled;
//
//   boolean runInNewThread=false;
//
//   private final Map<String, Consumer<CacheUpdateEvent>> changeEventSubscribers = new HashMap<>();
//
//   private boolean localCacheMap =false;
//
//   private static final String BOOLEAN_FALSE_STRING="false";
//
//   private static final int MAX_CACHE_MAP_SIZE=5000;
//
//
//   @PostConstruct
//   public void init() {
//      isFistLevelCacheEnabled=Boolean.parseBoolean(env.getProperty("cacheFirstLevel", BOOLEAN_FALSE_STRING));
//      localCacheMap = Boolean.parseBoolean(env.getProperty("redis.localCacheMap", BOOLEAN_FALSE_STRING));
//      String redisUrl = env.getProperty("redisUrl");
//      boolean clustered = Boolean.parseBoolean(env.getProperty("cacheClustered", BOOLEAN_FALSE_STRING));
//      int subscriptionConnectionPoolSize= Integer.parseInt(env.getProperty("subscriptionConnectionPoolSize", "250"));
//      int subscriptionsPerConnection= Integer.parseInt(env.getProperty("subscriptionsPerConnection", "25"));
//
//      if (StringUtils.isNotBlank(redisUrl)) {
//         Config config = new Config().setCodec(getCodec());
//         if (clustered) {
//            config.useClusterServers().setTimeout(30000)
//                    .setRetryAttempts(5)
//                    .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
//                    .setSubscriptionsPerConnection(subscriptionsPerConnection)
//                    .addNodeAddress(redisUrl);
//         } else {
//            config.useSingleServer().
//                    setTimeout(30000)
//                    .setRetryAttempts(5)
//                     .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
//                     .setSubscriptionsPerConnection(subscriptionsPerConnection)
//                    .setAddress(redisUrl);
//         }
//         int cleanupmindelay= Integer.parseInt(env.getProperty("cacheEvictionSchedulerMinDelay", "900"));
//         int cleanupmaxdelay= Integer.parseInt(env.getProperty("cacheEvictionSchedulerMaxDelay", "1800"));
//         config.setMinCleanUpDelay(cleanupmindelay);
//         config.setMaxCleanUpDelay(cleanupmaxdelay);
//
//         redisson = Redisson.create(config);
//      }
//      subscribeForChangeEvents();
//
//   }
//
//   private Codec getCodec(){
//      return new ByteArrayCodec(){
//         private final Encoder encoder = in -> {
//            if(in instanceof byte[]) {
//               return Unpooled.wrappedBuffer((byte[]) in);
//            }else{
//               return Unpooled.wrappedBuffer(SerializationUtils.serialize(((Serializable) in)));
//            }
//         };
//
//         private final Decoder<Object> decoder = (buf, state) -> {
//            byte[] result = new byte[buf.readableBytes()];
//            buf.readBytes(result);
//
//            return SerializationUtils.deserialize(result);
//         };
//
//         @Override
//         public Encoder getValueEncoder() {
//            return encoder;
//         }
//
//
//
//         @Override
//         public Decoder<Object> getValueDecoder() {
//            return decoder;
//         }
//
//      };
//   }
//
//   private void subscribeForChangeEvents(){
//      if(redisson!=null) {
//         RTopic topic = redisson.getTopic(env()+"-"+UPDATE_PUBSUB_TOPIC);
//         topic.addListener(CacheUpdateEvent.class, (charSequence, event) -> {
//            logger.debug("Cache update Event {}, Domain ->{}, Key->{}", event.getLob(), event.getDomainName(), event.getKey());
//            try {
//               if (event instanceof LOBRegisterEvent) {
//                  SpringContext.getBean(StartupBooster.class).loadLob(event.getLob());
//               } else if(event instanceof AppCacheRemoveEvent){
//                  logger.info("Removing AppCacheManager cache for lob:{}", event.getLob());
//                  AppCacheManager.getInstance().removeAll(event.getLob());
//               } else {
//                  clearCacheOnEvent(event);
//               }
//            } catch (Exception e) {
//               logger.error("Could not listener cache change event", e);
//            }
//         });
//      }
//   }
//
//   private void clearCacheOnEvent(CacheUpdateEvent event) {
//      SecurityContextUtils.switchWithLOB(event.getLob(),()->{
//         if(event.getKey()!=null) {
//            AppCacheManager.getInstance()
//                    .removeByDomain(event.getDomainName(), event.getKey());
//         }else{
//            AppCacheManager.getInstance()
//                    .removeByDomain(event.getDomainName());
//         }
//         var cacheUpdateEventConsumer = changeEventSubscribers.get(event.getDomainName());
//         if (cacheUpdateEventConsumer != null) {
//            cacheUpdateEventConsumer.accept(event);
//         }
//         logger.debug("Cleared first level cache for key {}",event.getKey());
//         return true;
//      });
//   }
//
//   @WithSpan
//   public void publishChangeEvent(CacheUpdateEvent e){
//      if(redisson!=null) {
//         RTopic topic = redisson.getTopic(env() + "-" + UPDATE_PUBSUB_TOPIC);
//         long clientsReceivedMessage = topic.publish(e);
//         logger.debug("Cache published event ->{} for event:{}", clientsReceivedMessage, e);
//      }
//   }
//   private void logCached(String cacheName, Object item) {
//      logger.debug("cached {} {}", cacheName, item);
//   }
//
//
//   public void put(String lob, String domainName, Map<String,Object> dataMap) {
//      String cacheName = getCacheName(lob, domainName);
//      if (redisson != null) {
//         try {
//            RMap<String, Object> map = getMap(cacheName);
//            map.putAll(dataMap);
//         } catch (Exception e) {
//            logger.error("Distributed cache put for key {} failed with error:", cacheName, e);
//         }
//      }
//   }
//
//   public void put(String lob, String domainName, String key, Object obj, boolean isRaw) {
//      put(lob,domainName,key,obj,isRaw,false);
//   }
//
//   /***
//    *
//    * @param lob
//    * @param domainName
//    * @param key
//    * @param obj
//    * @param isRaw use raw object(not supported yet)
//    * @param isFistLevelCacheEnabled
//    */
//   public void put(String lob, String domainName, String key, Object obj, boolean isRaw,boolean isFistLevelCacheEnabled) {
//      String cacheName = getCacheName(lob, domainName);
//      if (redisson != null) {
//         try {
//            RMap<String, Object> map = getMap(cacheName);
//            logCached(cacheName, obj);
//            if(map.size()>MAX_CACHE_MAP_SIZE){
//               map.clear();
//            }
//            GlobalLock.withLock(key,k->map.fastPut(k, obj));
//            if((isFistLevelCacheEnabled||this.isFistLevelCacheEnabled) && lob!=null){
//               AppCacheManager.getInstance().put(cacheName, key, obj);
//            }
//         } catch (Exception e) {
//            logPutError(cacheName, key, obj, e);
//         }
//      } else {
//         AppCacheManager.getInstance().put(cacheName, key, obj);
//      }
//   }
//
//   public void put(String lob, String domainName, String key, Object obj, long ttl, TimeUnit timeUnit,boolean localCacheMap) {
//	  String cacheName = getCacheName(lob, domainName);
//      if (redisson != null) {
//         try {
//            RMap<String, Object> map = getMap(cacheName,localCacheMap);
//            logCached(cacheName, obj);
//            if(map.size()>MAX_CACHE_MAP_SIZE){
//               map.clear();
//            }
//            if(map instanceof RMapCache){
//               ((RMapCache)map).fastPut(key, obj, ttl, timeUnit);
//            }else{
//               map.fastPut(key, obj);
//            }
//
//         } catch (Exception e) {
//            logPutError(cacheName, key, obj, e);
//         }
//      } else {
//         AppCacheManager.getInstance().put(cacheName, key, obj);
//      }
//   }
//
//
//   private void logPutError(String cacheName, String key, Object object, Exception e) {
//      logger.error("Could not put the object for cacheName:{}, key:{}, item:{}", cacheName, key, object, e);
//   }
//
//   private void logGetError(String cacheName, String key, Exception e) {
//      logger.error("Could not get object from cacheName:{}, key:{}", cacheName, key, e);
//   }
//
//   @WithSpan
//   public void putStream(StoreKey key, byte[] inData) {
//      try {
//         inData = CompressionUtils.compress(inData);
//      } catch (IOException e1) {
//         logger.error("Exception happened while compressing the data for key:{}", key, e1);
//      }
//
//      String cacheName = getCacheName(key.getLob(), cacheStore);
//      List<Profile> profile = ProfileRegistry.INSTANCE.get(key.getLob(), "s3", cacheStore);
//      if (redisson != null && !profile.isEmpty()) {
//         try {
//            RMap<String, Object> map = getMap(cacheName);
//            MediaOperation mediaService = mediaServiceProvider.getByProfile(cacheStore);
//            mediaService.upload(key.getKey(), (new ByteArrayInputStream(inData)), (long) inData.length);
//            key.setLastModified(System.currentTimeMillis());
//            map.fastPut(key.getKey(), key);
//         } catch (Exception e) {
//            logPutError(cacheName, key.getKey(), "compressedInputStream", e);
//         }
//      }
//   }
//
//
//
//   @WithSpan
//   @SuppressWarnings("java:S1168")
//   public byte[] getStream(String lob,String key, boolean useExpiry) {
//	  String cacheName = getCacheName(lob, cacheStore);
//      List<Profile> profile = ProfileRegistry.INSTANCE.get(lob, "s3", cacheStore);
//      if (redisson != null && !profile.isEmpty()) {
//         try {
//            RMap<String, Object> map = getMap(cacheName);
//            StoreKey keyS = (StoreKey) map.get(key);
//
//            if (keyS != null) {
//               if(useExpiry && (System.currentTimeMillis()-keyS.getLastModified())>30*60*1000){
//                  return null;
//               }
//               MediaOperation mediaService = mediaServiceProvider.getByProfile(cacheStore);
//               byte[] outData = IOUtils.toByteArray(mediaService.download(key));
//               return CompressionUtils.decompress(outData);
//            }
//         } catch (Exception e) {
//            logGetError(cacheName, key, e);
//         }
//      }
//      return null;
//   }
//
//   @WithSpan
//   public void removeStream(String lob, String domainName) {
//	  String cacheName = getCacheName(lob, domainName);
//      if (redisson != null) {
//         try {
//            RMap<String, Object> map = getMap(cacheName);
//            map.keySet().removeIf(e -> domainName.equalsIgnoreCase(((StoreKey) map.get(e)).getDomain()));
//         } catch (Exception e) {
//            logger.error("Could not remove stream for cacheName:{}", cacheName, e);
//         }
//      }
//   }
//
//   public Map<String,Object>  get(String lob, String domainName, Collection<String> keys) {
//      String cacheName = getCacheName(lob, domainName);
//      if (redisson != null) {
//         try {
//            Map<String,Object> objects = new HashMap<>();
//            RMap<String, Object> map = getMap(cacheName);
//            keys.forEach(key->{
//               Object v = map.get(key);
//               if(v!=null) {
//                  objects.put(key,v);
//               }
//            });
//            return objects;
//         } catch (Exception e) {
//            logger.error("Could  not get cache value for key:{} ", cacheName, e);
//            return null;
//         }
//      } else {
//         return null;
//      }
//   }
//
//   public Object get(String lob, String domainName, String key, boolean isRaw) {
//      return get(lob,domainName,key,isRaw,false,localCacheMap);
//   }
//
//   private String makeGlobalDomainKey(String domainName, String key) {
//      return domainName + ":" + key;
//   }
//
//   @WithSpan
//   @SuppressWarnings("unchecked")
//   public <T> T getValueByKey(String domainName, String key, Supplier<T> valueSupplier) {
//      String lookupKey = makeGlobalDomainKey(domainName, key);
//      Object value = redisson.getBucket(lookupKey).get();
//      if (value == null) {
//         value = valueSupplier.get();
//         if (value != null) {
//            redisson.getBucket(lookupKey).set(value);
//         }
//      }
//      return (T) value;
//   }
//
//   @WithSpan
//   @SuppressWarnings("unchecked")
//   public <T> T getValueByKey(String domainName, String key) {
//      String lookupKey = makeGlobalDomainKey(domainName, key);
//      return (T) redisson.getBucket(lookupKey).get();
//   }
//
//   @WithSpan
//   public <T> void putValueByKey(String domainName, Map<String, T> keyValuePairs) {
//      keyValuePairs.forEach((key, value) -> {
//         String keyName = makeGlobalDomainKey(domainName, key);
//         redisson.getBucket(keyName).set(value);
//      });
//   }
//
//   /***
//    *
//    * @param lob
//    * @param domainName
//    * @param key
//    * @param isRaw isRaw use raw object(not supported yet)
//    * @param isFistLevelCacheEnabled
//    * @param localCacheMap
//    * @return
//    */
//   @WithSpan
//   public Object get(String lob, String domainName, String key, boolean isRaw,boolean isFistLevelCacheEnabled,boolean localCacheMap) {
//	    String cacheName = getCacheName(lob, domainName);
//      if (redisson != null) {
//         try {
//            Object object = null;
//            if((isFistLevelCacheEnabled||this.isFistLevelCacheEnabled) && lob!=null && key !=null){
//               object=  AppCacheManager.getInstance().get(cacheName, key);
//            }
//            if(object==null){
//                  RMap<String, Object> map = getMap(cacheName,localCacheMap);
//                  object = map.get(key);
//            }
//            return object;
//         } catch (Exception e) {
//            logGetError(cacheName, key, e);
//            return null;
//         }
//      } else {
//         return AppCacheManager.getInstance().get(cacheName, key);
//      }
//   }
//
//   private void logError(String cacheName, Exception e) {
//      logger.error("Exception happened while accessing the cacheName:{}", cacheName, e);
//   }
//
//   @WithSpan
//   public Map<String, Object> get(String lob, String domainName) {
//	  String cacheName = getCacheName(lob, domainName);
//      HashMap<String, Object> dataMap = new HashMap<>();
//      if (redisson != null) {
//         try {
//            RMap<String, Object> map = getMap(cacheName);
//            map.keySet().forEach(k -> dataMap.put(k, map.get(k)));
//         } catch (Exception e) {
//            logError(cacheName, e);
//            return null;
//         }
//      }
//      return dataMap;
//   }
//
//   public Map<String, Object> resolveAndGet(String cacheName) {
//      return get(getCacheName(cacheName));
//   }
//   @WithSpan
//   public Map<String, Object> get(String cacheName) {
//      HashMap<String, Object> dataMap = new HashMap<>();
//      if (redisson != null) {
//         try {
//            RMap<String, Object> map = getMap(cacheName);
//            map.keySet().forEach(k -> dataMap.put(k, map.get(k)));
//         } catch (Exception e) {
//            logError(cacheName, e);
//            return null;
//         }
//      }
//      return dataMap;
//   }
//
//   @WithSpan
//   public void clearCache(String lob, String domainName, String key) {
//      clearCache(lob,domainName,key,localCacheMap);
//   }
//
//   @WithSpan
//   public void clearCache(String lob, String domainName, String key,boolean localCacheMap) {
//      String cacheName = getCacheName(lob, domainName);
//      if (redisson != null) {
//         try {
//            RMap<String, Object> map = getMap(cacheName,localCacheMap);
//            if(map.containsKey(key)) {
//               map.remove(key);
//               AppCacheManager.getInstance().remove(cacheName, key);
//               sendCacheUpdateEvent(domainName, lob, key);
//            }
//         } catch (Exception e) {
//            logError(cacheName, e);
//         }
//      } else {
//         AppCacheManager.getInstance().remove(cacheName, key);
//      }
//   }
//
//   private void sendCacheUpdateEvent(String domainName,String lob,String key){
//      try {
//         CacheUpdateEvent cue = new CacheUpdateEvent();
//         cue.setDomainName(domainName);
//         cue.setLob(lob);
//         cue.setKey(key);
//      } catch (Exception e) {
//         logger.error("Could not send cache update event for domainName:{}, key:{}", domainName, key, e);
//      }
//   }
//
//   @WithSpan
//   public void clearCache(String lob, String domainName) {
//	   String cacheName = getCacheName(lob, domainName);
//	   if (redisson != null) {
//		   try {
//			   RMap<String, Object> map = getMap(cacheName);
//			   map.clear();
//		   } catch (Exception e) {
//			   logError(cacheName, e);
//		   }
//	   } else {
//		   AppCacheManager.getInstance().clearCache(lob,cacheName);
//	   }
//   }
//
//   @WithSpan
//   @SuppressWarnings("unchecked")
//   public <V> V withCache(String lob, String key, Function<String, V> function) {
//      V cached = (V) get(lob, null, key, false);
//      if (cached == null) {
//         cached = SecurityContextUtils.switchWithLOB(lob, () -> function.apply(key), runInNewThread);
//         if (cached != null) {
//            put(lob, null, key, cached, false);
//         }
//      }
//      return cached;
//   }
//
//   @WithSpan
//   @SuppressWarnings("unchecked")
//   public <V> V withCache(String lob,@SpanAttribute("cacheDomain") String domain,@SpanAttribute("cacheKey") String key, Function<String, V> function) {
//      V cached = (V) get(lob, domain, key, false);
//      if (cached == null) {
//         cached = SecurityContextUtils.switchWithLOB(lob, () -> function.apply(key), runInNewThread);
//         if (cached != null) {
//            put(lob, domain, key, cached, false);
//         }
//      }
//      return cached;
//   }
//
//   @WithSpan
//   @SuppressWarnings("unchecked")
//   public <V> V withExpiringCache(String lob, String domain, String key, long ttl, TimeUnit unit, Function<String, V> function) {
//      V cached = (V) get(lob, domain, key, false,false,false);
//      if (cached == null) {
//         cached = SecurityContextUtils.switchWithLOB(lob, () -> function.apply(key), runInNewThread);
//         if (cached != null) {
//            put(lob, domain, key, cached, ttl, unit,false);
//         }
//      }
//      return cached;
//   }
//
//   private boolean isRoot(){
//      String lob=SecurityContextUtils.getLob();
//      return lob == null || ("default".equalsIgnoreCase(lob) || "root".equalsIgnoreCase(lob));
//   }
//   @WithSpan
//   public List<String> getAllKeys() {
//      List<String> keyList = new ArrayList<>();
//      if (redisson != null) {
//         RKeys keys = redisson.getKeys();
//         boolean isRoot=isRoot();
//         String lob=isRoot?null:SecurityContextUtils.getLob().toLowerCase();
//         keys.getKeys().forEach(k -> {
//            if(isRoot){
//               if (k.toLowerCase().startsWith(env())){
//                  keyList.add(k);
//               }
//            }else{
//               if (k.toLowerCase().contains(lob)) {
//                  keyList.add(k);
//               }
//            }
//         });
//      }
//      return keyList;
//   }
//
//   @WithSpan
//   public List<StoreKey> getStreams(String lob) {
//	  String cacheName = getCacheName(lob, cacheStore);
//      List<StoreKey> storeKeys = new ArrayList<>();
//      if (redisson != null) {
//         try {
//            RMap<String, Object> map = getMap(cacheName);
//            map.keySet().forEach(k -> storeKeys.add((StoreKey) map.get(k)));
//         } catch (Exception e) {
//            logError(cacheName, e);
//         }
//      }
//      return storeKeys;
//   }
//   @WithSpan
//   public void deleteKey(List<String> keys) {
//      if (redisson != null) {
//         redisson.getKeys().delete(keys.toArray(new String[0]));
//      }
//      keys.forEach(key -> {
//         RMap<String, Object> map = cmc.remove(key);
//         if (map != null) {
//            map.clear();
//         }
//      });
//   }
//   @WithSpan
//   public void deleteRedisCacheMap(List<String> keys) {
//      if (keys.isEmpty()) {
//         keys.addAll(getAllKeys());
//      }
//      keys.forEach(key -> {
//         RMap<String, Object> map = cmc.remove(key);
//         if (map != null) {
//            map.clear();
//         }
//      });
//   }
//
//
//   private String envName(String cacheName) {
//      return env()+":"+ cacheName;
//   }
//
//   private String env(){
//      return env.getProperty("channelkart.environment","dev");
//   }
//
//
//   private <T> RMap<String, T> getMap(String cacheName) {
//      return getMap(cacheName,localCacheMap);
//   }
//
//   private <T> RMap<String, T> getMap(String cacheName,boolean localCacheMap) {
//
//      RMap<String, T> rmap= (RMap<String, T>) cmc.get(cacheName);
//      if(rmap==null) {
//         if (localCacheMap)
//            rmap = redisson.getLocalCachedMap(cacheName, LocalCachedMapOptions.defaults());
//         else
//            rmap = redisson.getMapCache(cacheName);
//
//         cmc.put(cacheName, (RMap<String, Object>) rmap);
//      }
//      return rmap;
//   }
//   @WithSpan
//   public boolean deleteAll() {
//      try {
//         if (redisson != null) {
//            List<String> allKeys = getAllKeys();
//            if (!allKeys.isEmpty()) {
//               redisson.getKeys().delete(allKeys.toArray(new String[0]));
//            }
//            allKeys.forEach(key -> {
//               RMap<String, Object> map = cmc.remove(key);
//               if (map != null) {
//                  map.clear();
//               }
//            });
//         }
//         return true;
//      } catch (Exception e) {
//         logger.error("Could not clear distributed cache");
//         return false;
//      }
//   }
//
//   /**
//    * Gets the cache name.
//    *
//    * @param lob the lob
//    * @param domainName the domain name
//    * @return the cache name
//    */
//   @WithSpan
//   public String getCacheName(String lob,String domainName) {
//	   String cacheName = DataSourceUtils.isDefaultDataSource(lob) ? DEFAULT_CACHE_NAME : lob;
//	   cacheName = domainName == null ? cacheName : cacheName+":"+domainName ;
//	   cacheName = envName(cacheName);
//	   return cacheName;
//   }
//
//   /**
//    * Gets the cache name.
//    *
//    * @param domainName the domain name
//    * @return the cache name
//    */
//   @WithSpan
//   public String getCacheName(String domainName) {
//      String lob = SecurityContextUtils.getLob();
//      return getCacheName(lob,domainName);
//   }
//
//   /**
//    * Publish cache event.
//    *
//    * @param lob the lob
//    * @param key the key
//    * @param operation the operation
//    * @param data the data
//    */
//   @WithSpan
//   public void publishCacheEvent(String lob, String key, CacheOperationsConstant operation,Object data) {
//	   AppCacheEvent.Builder builder= new AppCacheEvent.Builder(operation, null)
//			   .setKey(key)
//			   .setLob(lob)
//			   .setData(data);
//	   AppCacheEventPublisher.get().publishEvent(builder, ()->null);
//   }
//   @WithSpan
//   public void count(String name){
//      if(redisson!=null){
//        String key= env()+":"+SecurityContextUtils.getLob()+":"+name;
//        redisson.getAtomicLong(key).incrementAndGet();
//      }
//   }
//   @WithSpan
//   public Long getCount(String name){
//      if(redisson!=null){
//         String key= env()+":"+SecurityContextUtils.getLob()+":"+name;
//         return redisson.getAtomicLong(key).get();
//      }
//      return 0L;
//   }
//   @WithSpan
//   public Long getCount(String lob,String name){
//      if(redisson!=null){
//         String key= env()+":"+lob+":"+name;
//         return redisson.getAtomicLong(key).get();
//      }
//      return 0L;
//   }
//   @WithSpan
//   public void addChangeEventListener(String domain, Consumer<CacheUpdateEvent> consumer) {
//      changeEventSubscribers.put(domain, consumer);
//   }
//
//   @WithSpan
//   public <T> void putKeyValue(String key, T value) {
//      redisson.getBucket(key).set(value);
//   }
//   @WithSpan
//   public boolean removeByKey(String key) {
//      return redisson.getBucket(key).delete();
//   }
//
//   @SuppressWarnings("unchecked")
//   @WithSpan
//   public <T> T getValue(String key) {
//      return (T) redisson.getBucket(key).get();
//   }
//
//   @WithSpan
//   public <T> T withLock(String lockable,Supplier<T> supplier){
//      RLock lock = redisson.getLock(lockable);
//      lock.lock();
//      try {
//        return supplier.get();
//      } finally {
//         lock.unlock();
//      }
//   }
//
//
//}
