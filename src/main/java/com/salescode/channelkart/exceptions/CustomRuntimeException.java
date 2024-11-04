/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.salescode.channelkart.exceptions;


import com.salescode.channelkart.utils.StringUtils;

/**
 * The class CustomRuntimeException.
 *
 * @author Manish Srivastava
 * @since  Sept 2020
 */
public class CustomRuntimeException extends RuntimeException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4825854136668962900L;

	/**
	 * Instantiates a new custom runtime exception.
	 *
	 * @param message the message
	 * @param values the values
	 */
	public CustomRuntimeException(String message, String ...values) {
		super(StringUtils.format(message, values));
	}

	/**
	 * Instantiates a new custom runtime exception.
	 *
	 * @param cause the cause
	 * @param message the message
	 * @param values the values
	 */
	public CustomRuntimeException(Throwable cause, String message, String ...values) {
		super(StringUtils.format(message, values), cause);
	}
	
	/**
	 * Gets the root cause.
	 *
	 * @param e the e
	 * @return the root cause
	 */
	public String getRootCause() {
	    Throwable cause = null; 
	    Throwable result = this;
	    while(null != (cause = result.getCause())  && (result != cause) ) {
	        result = cause;
	    }
	    return result.getMessage();
	}
	
	/**
	 * Instantiates a new custom runtime exception.
	 *
	 * @param cause the cause
	 */
	public CustomRuntimeException(Throwable cause) {
		super(cause.getMessage(), cause);
	}

}
