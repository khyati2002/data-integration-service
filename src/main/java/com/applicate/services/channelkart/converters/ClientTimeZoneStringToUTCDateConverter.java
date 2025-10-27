/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.converters;

import com.applicate.services.channelkart.exceptions.IllegalArgumentException;
import com.applicate.services.channelkart.validations.ValidationResponseMessage;
import com.fasterxml.jackson.databind.util.StdConverter;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;


public class ClientTimeZoneStringToUTCDateConverter extends StdConverter<String,Date> {
	
	/**
	 * Convert.
	 *
	 * @param value the value
	 * @return the string
	 */
   
	@Override
	public Date convert(String value) {
		if(value != null) {
			try {
				return convertTimeZone("yyyy-MM-dd HH:mm:ss", ZoneId.of(DateToClientTimeZoneStringConverter.getTimeZone()), StringUtils.normalizeSpace(value));
			} catch(DateTimeParseException e){
				throw new IllegalArgumentException(com.applicate.services.channelkart.utils.StringUtils.format(ValidationResponseMessage.INVALID_DATE_ERROR,value));
			}
		}
		return null;
	}
	

	private Date convertTimeZone(String dateFormat, ZoneId fromZone, String startDateStr) {
		DateTimeFormatter clientFormat =  DateTimeFormatter.ofPattern(dateFormat);
		LocalDateTime time = LocalDateTime.parse(startDateStr,clientFormat);
		ZonedDateTime currentclientTime = time.atZone(fromZone);       
        ZonedDateTime currentUTCTime = currentclientTime.withZoneSameInstant(ZoneId.of(ZoneId.systemDefault().toString()));
        return Date.from(currentUTCTime.toInstant());
	}
}
