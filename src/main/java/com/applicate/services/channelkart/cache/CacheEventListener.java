package com.applicate.services.channelkart.cache;

public interface CacheEventListener {
    void onCacheEvent(AppCacheEvent<?> event);
}
