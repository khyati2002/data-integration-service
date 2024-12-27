/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.salescode.channelkart.exceptions;


import com.salescode.channelkart.utils.StringUtils;

/**
 * The class UnexpectedResultException.
 *
 * @author Manish Srivastava
 * @since  Sept 2020
 */
public class UnexpectedResultException extends CustomRuntimeException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -2540716249558211158L;

	/**
	 * Instantiates a new unexpected result exception.
	 *
	 * @param message the message
	 * @param values the values
	 */
	public UnexpectedResultException(String message, String ...values) {
		super(StringUtils.format(message, values));
	}

	/**
	 * Instantiates a new unexpected result exception.
	 *
	 * @param cause the cause
	 * @param message the message
	 * @param values the values
	 */
	public UnexpectedResultException(Throwable cause, String message, String ...values) {
		 super(cause,StringUtils.format(message, values));
	}
	
}
