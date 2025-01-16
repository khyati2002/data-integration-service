/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.utils;

import org.springframework.core.NestedRuntimeException;

/**
 * The class ExceptionUtils.
 *
 * @author Manish Srivastava
 * @since  Nov 2020
 */
public class ExceptionUtils {

	private static final String empty= "";

	public static String getRootCauseMessage(Throwable input) {
		Throwable result = getRootCause(input);
		if(result == null) {
			return empty;
		}
		return result.getMessage();
	}

	public static Throwable getRootCause(Throwable input) {
		if(input != null) {
			synchronized(input) {
				Throwable cause = null;
				Throwable result = input;
				while(null != (cause = result.getCause())  && (result != cause) ) {
					result = cause;
				}
				return result;
			}
		}
		return null;
	}

	/*
	 * This method checks if exception contains given class type or has
	 * its child type.
	 *
	 *  @see MDMService
	 *  @author Manish Srivastava
	 * */
	public static boolean contains(Throwable throwable, Class<?> exType) {
		if(throwable == null) {
			return false;
		}
		synchronized(throwable) {
			if (exType == null) {
				return false;
			}
			if (exType.isInstance(throwable)) {
				return true;
			}
			Throwable cause = throwable.getCause();
			if (cause == throwable) {
				return false;
			}
			if (cause instanceof NestedRuntimeException) {
				return ((NestedRuntimeException) cause).contains(exType);
			}
			else {
				while (cause != null) {
					if (exType.isInstance(cause)) {
						return true;
					}
					if (cause.getCause() == cause) {
						break;
					}
					cause = cause.getCause();
				}
				return false;
			}
		}
	}

}
