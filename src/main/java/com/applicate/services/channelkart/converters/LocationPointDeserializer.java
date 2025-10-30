package com.applicate.services.channelkart.converters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.io.IOException;

public class LocationPointDeserializer extends JsonDeserializer<Point> {

	private final GeometryFactory factory = new GeometryFactory();

	/**
	 * Deserialize a JSON object with latitude and longitude into a JTS Point.
	 *
	 * @param jsonParser the JSON parser
	 * @param ctxt       the deserialization context
	 * @return the Point object
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public Point deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
		JsonNode node = jsonParser.getCodec().readTree(jsonParser);
		double latitude = node.get("latitude").asDouble();
		double longitude = node.get("longitude").asDouble();
		Point point = factory.createPoint(new Coordinate(longitude, latitude));
		point.setSRID(4326);
		return point;
	}
}
