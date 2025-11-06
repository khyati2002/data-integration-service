/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.exceptions;
import com.applicate.services.channelkart.utils.StringUtils;

/**
 * The class ValidationException.
 *
 * @author Manish Srivastava
 * @since  Oct 2020
 */
public class ValidationException extends RuntimeException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new validation exception.
	 *
	 * @param message the message
	 * @param values the values
	 */
	public ValidationException(String message,String ...values) {
		super(StringUtils.format(message, values));
	}

}
