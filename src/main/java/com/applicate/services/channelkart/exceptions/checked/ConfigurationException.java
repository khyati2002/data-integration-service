/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.exceptions.checked;


import com.applicate.services.channelkart.utils.StringUtils;

/**
 * The class ConfigurationException.
 *
 * @author Manish Srivastava
 * @since  Dec 2020
 */
public class ConfigurationException extends CustomCheckedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2823948932503702142L;

	/**
	 * Instantiates a new configuration exception.
	 *
	 * @param message the message
	 * @param values the values
	 */
	public ConfigurationException(String message, String ...values) {
		super(StringUtils.format(message, values));
	}

	/**
	 * Instantiates a new configuration exception.
	 *
	 * @param cause the cause
	 * @param message the message
	 * @param values the values
	 */
	public ConfigurationException(Throwable cause, String message, String ...values) {
		 super(cause,StringUtils.format(message, values));
	}
	
}
