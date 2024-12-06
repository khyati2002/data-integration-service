package com.salescode.channelkart.utils;

import org.json.JSONException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    private StringUtils() {
    }

    public static String format(final String str, Object... values) throws JSONException {
        synchronized (str.intern()) {
            Pattern pattern = Pattern.compile("\\{[^}]*}", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(str);
            int index = 0;
            String output = str;
            while (matcher.find()) {
                StringBuilder stringBuilder = new StringBuilder(output);
                String value = "";
                if (values.length > index) {
                    value = String.valueOf(values[index]);
                } else {
                    break;
                }
                stringBuilder.replace(matcher.start(), matcher.end(), value);
                output = stringBuilder.toString();
                matcher = pattern.matcher(output);
                index++;
            }
            return output;
        }
    }

    public static String escapeSql(String input) {
        input = input.replace("\\", "\\\\");
        return input.replace("'", "''");
    }

    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    public static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
    public static boolean isNotBlank(String input) {
        return input != null && !input.isBlank()	;
    }

    public static boolean isEqual(String firstValue, String secondValue, boolean ignoreCase) {
        if(ignoreCase) {
            return firstValue.equalsIgnoreCase(secondValue);
        }
        return firstValue.equals(secondValue);
    }

    public static boolean isNullOrBlank(Object value) {
        return (value == null) || (String.valueOf(value).isBlank());
    }

    public static boolean hasNullOrEmptyValues(String... values) {
        for (String value : values) {
            if(value == null || value.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}