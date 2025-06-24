package com.applicate.services.channelkart.converters;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonParser;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationContext;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class CustomLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Override
	public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		String value = p.getText();
		if (value == null || value.trim().isEmpty()) return null;

		try {
			return LocalDateTime.parse(value.trim(), formatter);
		} catch (DateTimeParseException e) {
			throw new IOException("Failed to parse LocalDateTime from '" + value + "'. Expected format: yyyy-MM-dd HH:mm:ss", e);
		}
	}
}
