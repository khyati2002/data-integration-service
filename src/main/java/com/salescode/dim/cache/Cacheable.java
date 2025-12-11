// 1. Cacheable annotation
package com.salescode.dim.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cacheable {
    /**
     * Cache name to use for storing results
     */
    String cacheName() default "dataintegration";

    /**
     * Time to live in minutes
     */
    int expireAfterMinutes() default 15;

    /**
     * Maximum size of the cache
     */
    int maximumSize() default 100;
}
