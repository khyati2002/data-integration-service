package com.salescode.dim.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Utility class that initializes a cache once at class load time.
 */
public final class CacheUtility {

    // The static cache instance, using String as the key and Object as the value.
    // You can adjust the types or add additional methods for type-safety if needed.
    private static final Cache<String, Object> CACHE;

    // Static initializer to build the cache once.
    static {
        CACHE = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .build();
    }

    // Private constructor prevents instantiation.
    private CacheUtility() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    /**
     * Retrieves the value for the given key from the cache.
     * If the key is not present, the supplier is used to compute and cache the value.
     *
     * @param key      The key for which the value is requested.
     * @param supplier The supplier to compute the value if absent.
     * @param <T>      The type of the value.
     * @return The cached or computed value.
     */
    @SuppressWarnings("unchecked")
    public static <T> T withCache(String key, Supplier<T> supplier) {
        return (T) CACHE.get(key, k -> supplier.get());
    }

    public static <T> T withCache(Class<?> clazz, String methodName, Supplier<T> supplier) {
        String key = clazz.getName() + "." + methodName;
        return withCache(key, supplier);
    }
}