/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.salescode.channelkart.exceptions.checked;


import com.salescode.channelkart.utils.StringUtils;

/**
 * The class CustomCheckedException.
 *
 * @author Manish Srivastava
 * @since  Dec 2020
 */
public class CustomCheckedException extends Exception {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 6367964491351572092L;

	/**
	 * Instantiates a new custom checked exception.
	 *
	 * @param message the message
	 * @param values the values
	 */
	public CustomCheckedException(String message, String ...values) {
		super(StringUtils.format(message, values));
	}

	
	/**
	 * Instantiates a new custom checked exception.
	 *
	 * @param cause the cause
	 * @param message the message
	 * @param values the values
	 */
	public CustomCheckedException(Throwable cause, String message, String ...values) {
		super(StringUtils.format(message, values), cause);
	}
	
	/**
	 * Gets the root cause.
	 *
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
	 * Instantiates a new custom checked exception.
	 *
	 * @param cause the cause
	 */
	public CustomCheckedException(Throwable cause) {
		super(cause.getMessage(), cause);
	}

}
