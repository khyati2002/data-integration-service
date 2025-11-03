package com.applicate.services.channelkart.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {
    public static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    public static Date parse(String date) {
        return parse(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date parse(String date, String pattern) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return simpleDateFormat.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}

