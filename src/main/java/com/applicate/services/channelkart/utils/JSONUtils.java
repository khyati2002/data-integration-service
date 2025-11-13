/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.utils;

import com.applicate.services.channelkart.converters.CustomLocalDateTimeDeserializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.beanutils.ConversionException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.MapperFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The class JSONUtils.
 *
 * @author Manish Srivastava
 * @since May 2020
 */
public class JSONUtils {

	public static final TypeReference<Map<String, String>> STRING_VALUE_MAP_REFERENCE = new TypeReference<>() {
	};
	public static final TypeReference<Map<String, Object>> OBJECT_VALUE_MAP_REFERENCE = new TypeReference<>() {
	};
	private static final TypeReference<List<String>> LIST_STRING_REFERENCE = new TypeReference<>() {
	};
	/**
	 * The Constant OBJECT_MAPPER.
	 */
	private static ObjectMapper OBJECT_MAPPER;

	static {
		get();
	}

	private JSONUtils() {
	}

	private static ObjectMapper get() {
		if (OBJECT_MAPPER == null) {
			OBJECT_MAPPER = new ObjectMapper();
			OBJECT_MAPPER.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
			OBJECT_MAPPER.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
			OBJECT_MAPPER.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
			OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
			JavaTimeModule module = new JavaTimeModule();
			module.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());
			OBJECT_MAPPER.registerModule(module);
		}
		return OBJECT_MAPPER;
	}

	public static ObjectMapper getObjectMapper() {
		return get();
	}

	public static JsonNode mergeJsonNodes(JsonNode source, JsonNode destination) throws IOException {
		ObjectNode destinationNode = destination.deepCopy();
		Iterator<String> fieldNames = source.fieldNames();
		while (fieldNames.hasNext()) {
			String fieldName = fieldNames.next();
			JsonNode jsonNode = destinationNode.get(fieldName);
			if (jsonNode != null && jsonNode.isObject()) {
				JsonNode value = mergeJsonNodes(source.get(fieldName), jsonNode);
				destinationNode.set(fieldName, value);
			} else {
				if (destinationNode instanceof ObjectNode) {
					JsonNode value = source.get(fieldName);
					destinationNode.set(fieldName, value);
				}
			}
		}
		return destinationNode;
	}

	public static ArrayNode convertToArrayNode(JsonNode jsonNode) {
		ArrayNode arrayNode;
		if (jsonNode.isArray()) {
			// If it's already an ArrayNode, cast and return
			return (ArrayNode) jsonNode;
		} else {
			// Create a new ArrayNode and add the current JsonNode
			arrayNode = JsonNodeFactory.instance.arrayNode().add(jsonNode);
			return arrayNode;
		}
	}

	public static <T> T parse(String data, Class<T> tClass) {
		try {
			return OBJECT_MAPPER.readValue(data, tClass);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T convert(Object node, TypeReference<List<Map<String, String>>> typeReference) {
		return (T) OBJECT_MAPPER.convertValue(node, typeReference);
	}

	public static JsonNode toJsonNode(Map<?, ?> input) {
		return OBJECT_MAPPER.convertValue(input, JsonNode.class);
	}
    public static boolean isNull(JsonNode node) {
        return node == null || node.isNull();
    }

	public static Stream<JsonNode> stream(JsonNode nodes) {
		return StreamSupport.stream(nodes.spliterator(), false);
	}
}