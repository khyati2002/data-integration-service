/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.exceptions.checked;

import com.applicate.services.channelkart.utils.StringUtils;

/**
 * The class MissingConfigurationException.
 *
 * @author Manish Srivastava
 * @since  Dec 2020
 */
public class MissingConfigurationException extends ConfigurationException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 5601360984917536461L;

	/**
	 * Instantiates a new missing configuration exception.
	 *
	 * @param message the message
	 * @param values the values
	 */
	public MissingConfigurationException(String message, String ...values) {
		super(StringUtils.format(message, values));
	}

	/**
	 * Instantiates a new missing configuration exception.
	 *
	 * @param cause the cause
	 * @param message the message
	 * @param values the values
	 */
	public MissingConfigurationException(Throwable cause, String message, String ...values) {
		 super(cause,StringUtils.format(message, values));
	}
	
}
