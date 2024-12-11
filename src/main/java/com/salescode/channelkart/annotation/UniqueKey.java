/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */

package com.salescode.channelkart.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The interface UniqueKey.
 *
 * @author Manish Srivastava
 * @since  Jun 2020
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface UniqueKey {
	
	/**
	 * Native name.
	 *
	 * @return the string
	 */
	String nativeName() default "";
	
	/**
	 * Path.
	 *
	 * @return the string
	 */
	String path() default "";
	
}
