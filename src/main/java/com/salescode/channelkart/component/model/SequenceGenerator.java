/*
*Copyright Applicate(2021) To Present
*
*All rights reserved
*/
package com.salescode.channelkart.component.model;


import com.salescode.channelkart.models.SequenceInfo;
import org.apache.commons.lang3.StringUtils;

/**
 * The interface SequenceGenerator.
 *
 * @author  Manish Srivastava
 * @since   Feb 2021
 */
public interface SequenceGenerator {

	/**
	 * The enum Value.
	 */
	public enum Value{
		STRING("auto_generated"),
		INTEGER("0"),
		PATTERN("APPCODE_");
		private final String defaultStr;
		private Value(String defaultStr) {
			this.defaultStr= defaultStr;
		}
		public String getDefaultValue() {
			return defaultStr;
		}
		public static Value getRegistry(String str) {
			if(StringUtils.isNotBlank(str)) {
				return Value.valueOf(str.toUpperCase());
			}
			throw new IllegalArgumentException("Value cannot be null or blank");
		}
	}
	
	/**
	 * Gets the value.
	 *
	 * @param sequenceInfo the sequence info
	 * @param entityObj the entity obj
	 * @param nextSequence the next sequence
	 * @return the value
	 */
	public String getValue(SequenceInfo sequenceInfo, Object entityObj, Integer nextSequence);

	/**
	 * Should modify.
	 *
	 * @return true, if successful
	 */
	public static boolean shouldModify(String data) {
		if(data == null) {
			return true;
		}
		return (StringUtils.isEmpty(data) || data.equals(Value.STRING.getDefaultValue()));
	}

	/**
	 * Checks if the given data start with provided pattern or not.
	 * If pattern is not provided, default pattern is selected.
	 * @param data the data to check
	 * @param pattern the pattern to match with
	 * @return boolean value
	 */
	static boolean matchesPattern(String data, String pattern){
		if(StringUtils.isEmpty(pattern)){
			pattern = Value.PATTERN.getDefaultValue();
		}
		return data.startsWith(pattern);
	}
	
}
