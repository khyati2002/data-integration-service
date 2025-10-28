/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.utils;

import com.applicate.services.channelkart.analytics.exception.ConverterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.firstDayOfYear;

/**
 * The class DateUtils.
 *
 * @author Manish Srivastava
 * @since  May 2020
 */
public class DateUtils {

	private static final String START_DATE = "STARTDATE";
	private static final String END_DATE = "ENDDATE";
	private static final String NUMBER_OF_DAYS = "NUMBEROFDAYS";

	public static final String DATE_FORMAT = "yyyy-MM-dd";

	public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final String STANDARD_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");
	public static final TimeZone IST_TIME_ZONE = TimeZone.getTimeZone("IST");

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);



	public static Date parse(String date) {
		return parse(date, STANDARD_DATE_TIME_FORMAT);
	}

	public static Date parse(String date, String pattern) {
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
			simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			return simpleDateFormat.parse(date);
		} catch (ParseException e) {
			throw new ConverterException(e);
		}
	}


}
