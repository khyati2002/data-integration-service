package com.applicate.services.channelkart.converters;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.applicate.services.channelkart.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import java.io.IOException;

public class JSONArrayConverter implements AttributeConverter<ArrayNode, String>
{
	private static final Logger log = LoggerFactory.getLogger(JSONArrayConverter.class);
	@Override
	public String convertToDatabaseColumn(ArrayNode attribute) {
		if (attribute != null) {
			return attribute.toString();
		} else {
			return null;
		}
	}

	@Override
	public ArrayNode convertToEntityAttribute(String dbData) {
		if (dbData != null) {
			try {
				return (ArrayNode) JSONUtils.getObjectMapper().readTree(dbData);
			} catch (IOException e) {
				log.error("stacktrace", e);
			}
		} 
		return null;
		
	}


}
