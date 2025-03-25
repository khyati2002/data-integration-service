// 4. Main Caching Aspect
package com.salescode.dim.cache;

import com.github.benmanes.caffeine.cache.Cache;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RMapCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Aspect
public class CachingAspect {
    private static final Logger logger = LoggerFactory.getLogger(CachingAspect.class);
    private final CacheManager cacheManager = CacheManager.getInstance();
    private final Executor asyncExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Around("@annotation(com.salescode.dim.cache.Cacheable) && execution(* *(..))")
    public Object cacheable(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        Cacheable cacheableAnnotation = method.getAnnotation(Cacheable.class);
        String cacheName = cacheableAnnotation.cacheName();
        int maximumSize = cacheableAnnotation.maximumSize();
        int expireAfterMinutes = cacheableAnnotation.expireAfterMinutes();

       RMapCache<String, Object> cache = cacheManager.getCache(cacheName,expireAfterMinutes);

        String key = generateCacheKey(pjp);

       // logger.info("Checking cache for method: {}", method.getName());

        // Try to get from cache
        Object cachedResult = cache.get(key);
        if (cachedResult != null) {
            logger.info("Cache hit for key: {}", key);
            return cachedResult;
        }

        logger.info("Cache miss for key: {}. Executing method: {}", key, method.getName());
    //     Execute the method and cache the result
        Object result = pjp.proceed();

        // Don't cache null results
        if (result != null) {
            cache.put(key, result);
           logger.info("Caching result for key: {}", key);
        }

        return result;
    }

    @Around("@annotation(com.salescode.dim.cache.CacheEvict) && execution(* *(..))")
    public Object cacheEvict(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        CacheEvict evictAnnotation = method.getAnnotation(CacheEvict.class);
        String cacheName = evictAnnotation.cacheName();
        boolean allEntries = evictAnnotation.allEntries();
        String keyPattern = evictAnnotation.keyPattern();

        logger.info("Evicting cache for method: {}", method.getName());

        // Execute the method first
        Object result = pjp.proceed();

        // Perform cache eviction asynchronously after method execution
        CompletableFuture.runAsync(() -> {
            if (allEntries) {
                logger.info("Evicting all entries in cache: {}", cacheName);
                cacheManager.evictAll(cacheName);
            } else if (keyPattern != null && !keyPattern.isEmpty()) {
                logger.info("Evicting cache entries matching pattern: {} in cache: {}", keyPattern, cacheName);
                cacheManager.evictByPattern(cacheName, keyPattern);
            } else {
                // Evict based on the method call
                String key = generateCacheKey(pjp);
                logger.info("Evicting specific key: {} in cache: {}", key, cacheName);
                cacheManager.getCache(cacheName, 10).remove(key);
            }
        }, asyncExecutor);

        return result;
    }

    private String generateCacheKey(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(method.getDeclaringClass().getName())
                  .append(".")
                  .append(method.getName())
                  .append("(");

        // Add parameter types to make the signature more specific
        for (Class<?> paramType : method.getParameterTypes()) {
            keyBuilder.append(paramType.getName()).append(",");
        }

        if (method.getParameterTypes().length > 0) {
            keyBuilder.setLength(keyBuilder.length() - 1); // Remove last comma
        }

        keyBuilder.append(")");

        // Add arguments hash for the specific call
        if (pjp.getArgs().length > 0) {
            keyBuilder.append("_").append(Arrays.deepHashCode(pjp.getArgs()));
        }

        return keyBuilder.toString();
    }
}