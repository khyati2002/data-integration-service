package com.applicate.services.channelkart.converters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.awt.*;
import java.io.IOException;

/**
 * Custom Jackson serializer for JTS Point.
 * Converts a Point geometry into JSON with "latitude" and "longitude" fields.
 */
public class LocationPointSerializer extends JsonSerializer<Point> {

	@Override
	public void serialize(Point value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if (value == null) {
			gen.writeNull();
			return;
		}

		gen.writeStartObject();
		gen.writeNumberField("latitude", value.getY());
		gen.writeNumberField("longitude", value.getX());
		gen.writeEndObject();
	}
}

