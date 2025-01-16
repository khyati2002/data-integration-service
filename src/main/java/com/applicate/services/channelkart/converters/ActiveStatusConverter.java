/*
 * Copyright (c) 2021. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.converters;


import com.applicate.services.channelkart.models.enums.ActiveStatus;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * The Class ActiveStatusConverter.
 *
 * @author Manish Srivastava
 * @since  Dec 2021
 */
@Component
@Converter(autoApply = true)
public class ActiveStatusConverter implements AttributeConverter<ActiveStatus, String> {

	/**
	 * Convert to database column.
	 *
	 * @param attribute the attribute
	 * @return the string
	 */
	@Override
	public String convertToDatabaseColumn(ActiveStatus attribute) {
		if(attribute != null) {
			return attribute.getStatus();
		}
		return ActiveStatus.INACTIVE.getStatus();
	}

	/**
	 * Convert to entity attribute.
	 *
	 * @param dbData the db data
	 * @return the active status
	 */
	@Override
	public ActiveStatus convertToEntityAttribute(String dbData) {
		if(StringUtils.isNotBlank(dbData)) {
			return ActiveStatus.getRegistry(dbData);
		}
		return ActiveStatus.INACTIVE;
	}

}
