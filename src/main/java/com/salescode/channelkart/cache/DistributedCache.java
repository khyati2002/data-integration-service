/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.cache;


import com.salescode.channelkart.abstractdatasource.AbstractDataSourceConstants;
import com.salescode.channelkart.utils.GlobalLock;
import io.netty.buffer.Unpooled;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.client.codec.Codec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import org.redisson.config.Config;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class DistributedCache {

   private static final Logger logger = LoggerFactory.getLogger(DistributedCache.class);

   private static final String UPDATE_PUBSUB_TOPIC = "change-publisher";

   boolean isFistLevelCacheEnabled;

   private static final String DEFAULT_CACHE_NAME = AbstractDataSourceConstants.DEFAULT;

   private Map<String, RMap<String, Object>> cmc=new ConcurrentHashMap<>();

   @Autowired
   private Environment env;
   private static final String BOOLEAN_FALSE_STRING = "false";
   private boolean localCacheMap = false;

   private RedissonClient redisson;

   private static final int MAX_CACHE_MAP_SIZE=5000;

   boolean runInNewThread=false;


   @PostConstruct
   public void init() {
      isFistLevelCacheEnabled = Boolean.parseBoolean(env.getProperty("cacheFirstLevel", BOOLEAN_FALSE_STRING));
      localCacheMap = Boolean.parseBoolean(env.getProperty("redis.localCacheMap", BOOLEAN_FALSE_STRING));
      String redisUrl = env.getProperty("redisUrl");
      boolean clustered = Boolean.parseBoolean(env.getProperty("cacheClustered", BOOLEAN_FALSE_STRING));
      int subscriptionConnectionPoolSize = Integer.parseInt(env.getProperty("subscriptionConnectionPoolSize", "250"));
      int subscriptionsPerConnection = Integer.parseInt(env.getProperty("subscriptionsPerConnection", "25"));

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
         int cleanupmindelay = Integer.parseInt(env.getProperty("cacheEvictionSchedulerMinDelay", "900"));
         int cleanupmaxdelay = Integer.parseInt(env.getProperty("cacheEvictionSchedulerMaxDelay", "1800"));
         config.setMinCleanUpDelay(cleanupmindelay);
         config.setMaxCleanUpDelay(cleanupmaxdelay);

         redisson = Redisson.create(config);
      }
      //subscribeForChangeEvents();
   }
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

   public Object get(String lob, String domainName, String key, boolean isRaw) {
      return get(lob,domainName,key,isRaw,false,localCacheMap);
   }

   public static boolean isDefaultDataSource(String lob) {
      return (lob == null || lob.equals(AbstractDataSourceConstants.DEFAULT));
   }

   private String envName(String cacheName) {
      return env()+":"+ cacheName;
   }

   private String env(){
      return env.getProperty("channelkart.environment","dev");
   }

   @WithSpan
   public String getCacheName(String lob,String domainName) {
      String cacheName = isDefaultDataSource(lob) ? DEFAULT_CACHE_NAME : lob;
      cacheName = domainName == null ? cacheName : cacheName+":"+domainName ;
      cacheName = envName(cacheName);
      return cacheName;
   }

   @WithSpan
   public Object get(String lob, String domainName, String key, boolean isRaw,boolean isFistLevelCacheEnabled,boolean localCacheMap) {
      String cacheName = getCacheName(lob, domainName);
      if (redisson != null) {
         try {
            Object object = null;
            if((isFistLevelCacheEnabled||this.isFistLevelCacheEnabled) && lob!=null && key !=null){
               object=  AppCacheManager.getInstance().get(cacheName, key);
            }
            if(object==null){
               RMap<String, Object> map = getMap(cacheName,localCacheMap);
               object = map.get(key);
            }
            return object;
         } catch (Exception e) {
         //   logGetError(cacheName, key, e);
            return null;
         }
      } else {
         return AppCacheManager.getInstance().get(cacheName, key);
      }
   }

   private <T> RMap<String, T> getMap(String cacheName,boolean localCacheMap) {

      RMap<String, T> rmap= (RMap<String, T>) cmc.get(cacheName);
      if(rmap==null) {
         if (localCacheMap)
            rmap = redisson.getLocalCachedMap(cacheName, LocalCachedMapOptions.defaults());
         else
            rmap = redisson.getMapCache(cacheName);

         cmc.put(cacheName, (RMap<String, Object>) rmap);
      }
      return rmap;
   }

   @WithSpan
   @SuppressWarnings("unchecked")
   public <V> V withCache(String lob, @SpanAttribute("cacheDomain") String domain, @SpanAttribute("cacheKey") String key, Function<String, V> function) {
      V cached = (V) get(lob, domain, key, false);
      if (cached == null) {
         cached = function.apply(key);
         if (cached != null) {
            put(lob, domain, key, cached, false);
         }
      }
      return cached;
   }

   public void put(String lob, String domainName, String key, Object obj, boolean isRaw) {
      put(lob,domainName,key,obj,isRaw,false);
   }

   private <T> RMap<String, T> getMap(String cacheName) {
      return getMap(cacheName,localCacheMap);
   }
   public void put(String lob, String domainName, String key, Object obj, boolean isRaw,boolean isFistLevelCacheEnabled) {
      String cacheName = getCacheName(lob, domainName);
      if (redisson != null) {
         try {
            RMap<String, Object> map = getMap(cacheName);
           // logCached(cacheName, obj);
            if(map.size()>MAX_CACHE_MAP_SIZE){
               map.clear();
            }
            GlobalLock.withLock(key, k->map.fastPut(k, obj));
            if((isFistLevelCacheEnabled||this.isFistLevelCacheEnabled) && lob!=null){
               AppCacheManager.getInstance().put(cacheName, key, obj);
            }
         } catch (Exception e) {
         //logPutError(cacheName, key, obj, e);
         }
      } else {
         AppCacheManager.getInstance().put(cacheName, key, obj);
      }
   }

   @WithSpan
   public void clearCache(String lob, String domainName, String key) {
      clearCache(lob,domainName,key,localCacheMap);
   }

   @WithSpan
   public void clearCache(String lob, String domainName, String key,boolean localCacheMap) {
      String cacheName = getCacheName(lob, domainName);
      if (redisson != null) {
         try {
            RMap<String, Object> map = getMap(cacheName,localCacheMap);
            if(map.containsKey(key)) {
               map.remove(key);
               AppCacheManager.getInstance().remove(cacheName, key);
            //   sendCacheUpdateEvent(domainName, lob, key);
            }
         } catch (Exception e) {
           // logError(cacheName, e);
         }
      } else {
         AppCacheManager.getInstance().remove(cacheName, key);
      }
   }
}


