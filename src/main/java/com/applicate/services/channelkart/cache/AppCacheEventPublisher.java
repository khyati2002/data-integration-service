package com.applicate.services.channelkart.cache;

import com.applicate.services.channelkart.exceptions.IllegalArgumentException;
import com.applicate.services.channelkart.security.Function;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class AppCacheEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(AppCacheEventPublisher.class);

    private static AppCacheEventPublisher instance;


    private final DistributedCache distributedCache;

    private final List<RegisterCacheEvent> registries;

    private final List<CacheEventListener> listeners = new CopyOnWriteArrayList<>();


    public AppCacheEventPublisher(DistributedCache distributedCache,
                                  List<RegisterCacheEvent> registries) {
        this.distributedCache = distributedCache;
        this.registries = registries;
    }

    public void registerListener(CacheEventListener listener) {
        listeners.add(listener);
    }


    private void notifyListeners(AppCacheEvent<?> event) {
        for (CacheEventListener listener : listeners) {
            try {
                listener.onCacheEvent(event);
            } catch (Exception e) {
                logger.error("Listener error", e);
            }
        }
    }


    public void publishEvent(AppCacheEvent.Builder builder, Function<Object> function) {

        if (builder == null) {
            throw new IllegalArgumentException("Builder cannot be null");
        }

        AppCacheEvent<?> event = builder.build();

        if (StringUtils.isBlank(event.getKey())) {
            throw new IllegalArgumentException("Event key cannot be null/blank");
        }

        Class<?> clazz = event.getHandlerClass();
        if (clazz == null && lookUp(event.getKey()).isPresent()) {
            clazz = (Class<?>) lookUp(event.getKey()).orElse(null);
        }

        if (clazz != null) {
            register(event.getKey(), clazz);
            builder.setHandlerClass(clazz);

            if (logger.isDebugEnabled()) {
                logger.debug("Publishing event for {}, operation: {}",
                        event.getKey(), event.getType().name());
            }

            notifyListeners(builder.build());
        }

        function.invoke();
    }

    public void register(String key, Class<?> handlerClass) {
        if (StringUtils.isBlank(key) || handlerClass == null) {
            logger.warn("Cannot register cache handler for key: {}, handler: {}", key, handlerClass);
            return;
        }
        distributedCache.withCache(null, "appcacheventregistry", key, t -> handlerClass);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> lookUp(String key) {
        return (Optional<T>) Optional.ofNullable(
                distributedCache.get(null, "appcacheventregistry", key, true));
    }

    public void initRegistry() {
        if (ObjectUtils.isNotEmpty(registries)) {
            for (RegisterCacheEvent element : registries) {
                Map<String, Class<?>> map = element.register();
                if (ObjectUtils.isNotEmpty(map)) {
                    map.forEach(this::register);
                }
            }
        }
    }

    public static AppCacheEventPublisher get() {
        if (instance == null) {
            throw new IllegalStateException("AppCacheEventPublisher not initialized");
        }
        return instance;
    }

    public static synchronized void setInstance(AppCacheEventPublisher publisher) {
        instance = publisher;
    }

}
