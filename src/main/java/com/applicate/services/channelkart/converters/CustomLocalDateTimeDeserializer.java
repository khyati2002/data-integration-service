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

		String raw = p.getText();

		if (raw == null || raw.trim().isEmpty()) return null;

        raw=raw.trim();

        try {
            return LocalDateTime.parse(raw);
        } catch (Exception ignored) { }

        try {
            return LocalDateTime.parse(raw, formatter);
        } catch (Exception ignored) { }

		// Clean the weird value: "2025-03-07 00:00:00T00:00:00Z"
		// Take only the part before the 'T' if it exists

		String cleaned = raw.contains("T") ? raw.substring(0, raw.indexOf("T")) : raw;

		try {

			return LocalDateTime.parse(cleaned.trim(), formatter);

		} catch (DateTimeParseException e) {

			throw new IOException("Unable to parse LocalDateTime from: " + raw, e);

		}

	}

}