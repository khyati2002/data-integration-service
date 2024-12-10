package com.salescode.channelkart.validations.repository;

import java.util.regex.Pattern;

public class RegexValidation {
	
	public boolean match(String regex,String value) {
		return Pattern.matches(regex, value);
	}

}
