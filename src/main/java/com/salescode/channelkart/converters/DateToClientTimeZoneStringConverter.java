/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.salescode.channelkart.converters;


import com.fasterxml.jackson.databind.util.StdConverter;
import com.salescode.channelkart.models.CustomerAccountInfo;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.services.CustomerAccountsService;
import com.salescode.channelkart.services.ServiceLocator;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;


public class DateToClientTimeZoneStringConverter extends StdConverter<Date,String> {
	

	@Override
	public String convert(Date value) {
		if(value != null) {
			String timeZoneStr =getTimeZone();
			timeZoneStr = StringUtils.isEmpty(timeZoneStr)?"Asia/Kolkata":timeZoneStr;
			LocalDateTime local = value.toInstant().atZone(ZoneId.of(ZoneId.systemDefault().toString())).toLocalDateTime();
			ZonedDateTime currentUTCTime = local.atZone(ZoneId.systemDefault());       
			ZonedDateTime currentclientTime = currentUTCTime.withZoneSameInstant(ZoneId.of(timeZoneStr));
			return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(currentclientTime);
		}
		return null;
	}

	public static String getTimeZone() {
		String lob= SecurityContextUtils.getLob();
		try {
			if(lob!=null  && !lob.equals("default")) {
				CustomerAccountsService customerService = (CustomerAccountsService) ServiceLocator.lookup(CustomerAccountInfo.class);
				return customerService.getTimeZone();
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}
}
