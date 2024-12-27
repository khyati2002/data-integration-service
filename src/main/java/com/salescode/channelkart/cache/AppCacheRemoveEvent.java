package com.salescode.channelkart.cache;

public class AppCacheRemoveEvent extends CacheUpdateEvent{
    @Override
    public String toString() {
        return "AppCacheRemoveEvent{ lob:" + getLob() + "}";
    }
}
