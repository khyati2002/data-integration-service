
package com.applicate.services.channelkart.cache;

import com.applicate.services.channelkart.utils.SecurityContextUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;

public class AppCacheEvent<T>  {

    @Getter
    private String key;
    @Getter
    private Object data;
    @Getter
    private CacheOperationsConstant type;
    @Getter
    private String lob;
    private Class<?> handlerClass;
    public AppCacheEvent(String key, T data, CacheOperationsConstant type, String lob, Class<?> handlerClass) {
        this.key = key;
        this.data = data;
        this.type = type;
        this.lob = lob;
        this.handlerClass = handlerClass;
    }

    public ResolvedTypeInfo getResolvedTypeInfo() {
        return new ResolvedTypeInfo(getClass(), handlerClass);
    }

    @SuppressWarnings("unchecked")
    public <E> Class<E> getHandlerClass() {
        return (Class<E>) handlerClass;
    }


    @Getter
    public static class ResolvedTypeInfo {
        private final Class<?> eventClass;
        private final Class<?> handlerClass;

        public ResolvedTypeInfo(Class<?> eventClass, Class<?> handlerClass) {
            this.eventClass = eventClass;
            this.handlerClass = handlerClass;
        }

    }

    protected static class Builder {
        private String key;
        private Object data;
        private CacheOperationsConstant type;
        private String lob;
        @Setter
        private Class<?> handlerClass;

        protected Builder(CacheOperationsConstant type, Class<?> handlerClass) {
            this.type = type;
            this.handlerClass = handlerClass;
        }

        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        public Builder setData(Object data) {
            this.data = data;
            return this;
        }

        public Builder setType(CacheOperationsConstant type) {
            this.type = type;
            return this;
        }

        public Builder setLob(String lob) {
            this.lob = lob;
            return this;
        }
        @SuppressWarnings("unchecked")
        public <T> AppCacheEvent<T> build() {
            if (ObjectUtils.isEmpty(this.lob)) {
                this.lob = SecurityContextUtils.getLob();
            }
            return new AppCacheEvent<>(key, (T) data, type, lob, handlerClass);
        }
    }

}