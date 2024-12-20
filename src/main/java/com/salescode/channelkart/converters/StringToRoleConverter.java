/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.salescode.channelkart.converters;


import com.fasterxml.jackson.databind.util.StdConverter;
import com.salescode.channelkart.models.Role;
import com.salescode.channelkart.services.RoleService;
import com.salescode.channelkart.services.SpringContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StringToRoleConverter extends StdConverter<List<String>, List<Role>> {
	
	private RoleService roleService = SpringContext.getBean(RoleService.class);

	/**
	 * Convert.
	 *
	 * @param value the value
	 * @return the string
	 */
	@Override
	public List<Role> convert(List<String> value) {
		if (value != null && !value.isEmpty()) {
			return value.stream().map(roleService::getRoleAsList).flatMap(List::stream).collect(Collectors.toList());
		}
		return new ArrayList<>();
	}

}
