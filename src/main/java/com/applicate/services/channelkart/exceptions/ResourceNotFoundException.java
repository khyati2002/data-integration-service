/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.exceptions;

import com.applicate.services.channelkart.utils.StringUtils;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The class ResourceNotFoundException.
 *
 * @author Manish Srivastava
 * @since  Nov 2020
 */
//@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends ExecutionInteruptedException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new resource not found exception.
	 *
	 * @param message the message
	 * @param values the values
	 */
	public ResourceNotFoundException(String message, String ...values) {
		super(StringUtils.format(message, values));
	}

	/**
	 * Instantiates a new resource not found exception.
	 *
	 * @param cause the cause
	 * @param message the message
	 * @param values the values
	 */
	public ResourceNotFoundException(Throwable cause, String message, String ...values) {
		super(cause,StringUtils.format(message, values));
	}

}