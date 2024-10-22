package com.salescode.channelkart.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.channelkart.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class JSONObjectConverter implements AttributeConverter<JsonNode, String> {

	private static final Logger log = LoggerFactory.getLogger(JSONObjectConverter.class);

	@Override
	public String convertToDatabaseColumn(JsonNode attribute) {
		if (attribute != null) {
			return attribute.toString();
		} else {
			return null;
		}
	}

	@Override
	public JsonNode convertToEntityAttribute(String dbData) {
		if (dbData != null) {
			try {
				return JSONUtils.getObjectMapper().readTree(dbData);
			}catch (Exception e) {
				log.error("stacktrace", e);
				return null;
			}
		} else {
			return null;
		}
	}

}
