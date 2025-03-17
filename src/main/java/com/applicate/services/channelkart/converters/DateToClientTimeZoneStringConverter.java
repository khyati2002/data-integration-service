/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.converters;


import com.applicate.services.channelkart.services.CustomerAccountsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.salescode.dim.jooq.generated.tables.pojos.CustomerAccount;
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

		CustomerAccountsService customerService = (CustomerAccountsService) ServiceLocator.lookup(CustomerAccount.class);
		return customerService.getTimeZone();

	}
}
