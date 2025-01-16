/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.converters;


import com.fasterxml.jackson.databind.util.StdConverter;
import com.applicate.services.channelkart.models.User;

/**
 * The Class UserToStringConverter.
 *
 * @author Manish Srivastava
 * @since  Apr 2020
 */
public class UserToStringConverter extends StdConverter<User,String> {
	
	/**
	 * Convert.
	 *
	 * @param value the value
	 * @return the string
	 */
	@Override
	public String convert(User value) {
		if(value != null) {
			return value.getLoginId();
		}
		return null;
	}
	
}
