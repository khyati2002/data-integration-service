/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.converters;


import com.fasterxml.jackson.databind.util.StdConverter;
import com.applicate.services.channelkart.models.Role;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RoleToStringConverter extends StdConverter<List<Role>, List<String>> {

	/**
	 * Convert.
	 *
	 * @param value the value
	 * @return the string
	 */
	@Override
	public List<String> convert(List<Role> value) {
		if (value != null && !value.isEmpty()) {
			return value.stream().map(Role::getName).collect(Collectors.toList());
		}
		return new ArrayList<>();
	}

}
