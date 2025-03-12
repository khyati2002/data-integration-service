// 2. Cache eviction annotation
package com.salescode.dim.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheEvict {
    /**
     * Cache name to evict entries from
     */
    String cacheName() default "default";
    
    /**
     * Whether to evict all entries
     */
    boolean allEntries() default false;
    
    /**
     * Key pattern to evict (if not all entries)
     */
    String keyPattern() default "";
}