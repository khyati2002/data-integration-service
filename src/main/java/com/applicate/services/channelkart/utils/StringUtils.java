package com.applicate.services.channelkart.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
	public static boolean isNullOrBlank(Object value) {
		return (value == null) || (String.valueOf(value).isBlank());
	}

	public static boolean hasNullOrEmptyValues(String... values) {
		for (String value : values) {
			if (value == null || value.isEmpty()) {
				return true;
			}
		}
		return false;
	}


	public static boolean isValidString(String value) {
		return !isEmpty(value) && !value.equals("null") && !value.equals("\"\"") && !value.equalsIgnoreCase("undefined");
	}

	public static boolean isNotEmpty(String value) {
		return !isEmpty(value);
	}

	public static boolean isEmpty(String value) {
		return value == null || value.isEmpty();
	}

	public static boolean isValidString(String value) {
		return !isEmpty(value) && !value.equals("null") && !value.equals("\"\"") && !value.equalsIgnoreCase("undefined");
	}


	public static String format(final String str, Object... values) {
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
}
