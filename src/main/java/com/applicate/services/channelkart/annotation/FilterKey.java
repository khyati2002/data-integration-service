package com.applicate.services.channelkart.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The interface LoginId.
 *
 * @author Priyanshu Sharan
 * @since  May 2024
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface FilterKey {
    enum Key {
        LOGIN_ID,
        OUTLET_CODE
    }
    Key value() default Key.LOGIN_ID;
    boolean isImmediateParentAccess() default false;
    boolean isDefaultKey() default false;
}
