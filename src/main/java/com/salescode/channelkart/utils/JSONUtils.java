/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.ConversionException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The class JSONUtils.
 *
 * @author Manish Srivastava
 * @since May 2020
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Slf4j
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
        }
        return OBJECT_MAPPER;
    }

    public static ObjectMapper getObjectMapper() {
        return get();
    }

    public static JsonNode mergeJsonNodes(JsonNode source, JsonNode destination) throws JSONException, IOException {

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

    public static Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new LinkedHashMap<>();

        Iterator<String> keysItr = object.keys();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if (value instanceof JSONArray) {
                value = toJsonToArrayList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    public static List<Object> toJsonToArrayList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toJsonToArrayList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }
    public static Map<String, Object> toMap(Object input) {
        return getObjectMapper().convertValue(input, OBJECT_VALUE_MAP_REFERENCE);
    }
    public static JsonNode toJsonNode(Object item) {
        return get().convertValue(item, JsonNode.class);
    }
    public static boolean isNull(JsonNode node) {
        return node == null || node.isNull();
    }

    public static <T> T convert(JsonNode node, Class<T> clazz) {
        return OBJECT_MAPPER.convertValue(node, clazz);
    }

    public static <T> T convert(Object node, TypeReference<T> typeReference) {
        return OBJECT_MAPPER.convertValue(node, typeReference);
    }

    public static String stringify(Object object){
        try {
            return getObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("stacktrace", e);
        }
        return null;
    }


    public static Stream<JsonNode> stream(JsonNode nodes) {
        return StreamSupport.stream(nodes.spliterator(), false);
    }

    public static Collector<JsonNode, ArrayNode, ArrayNode> toArrayNode() {
        return new ArrayNodeCollector();
    }

    private static class ArrayNodeCollector implements Collector<JsonNode, ArrayNode, ArrayNode> {

        @Override
        public Supplier<ArrayNode> supplier() {
            return OBJECT_MAPPER::createArrayNode;
        }

        @Override
        public BiConsumer<ArrayNode, JsonNode> accumulator() {
            return ArrayNode::add;
        }

        @Override
        public BinaryOperator<ArrayNode> combiner() {
            return (x, y) -> {
                x.addAll(y);
                return x;
            };
        }

        @Override
        public Function<ArrayNode, ArrayNode> finisher() {
            return accumulator -> accumulator;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return EnumSet.of(Characteristics.UNORDERED);
        }
    }

    public static JsonNode parse(String data) {
        try {
            return OBJECT_MAPPER.readTree(data);
        } catch (JsonProcessingException e) {
            throw new ConversionException("Could not convert to json node. input" + data, e);
        }
    }

    public static String toJsonString(Collection<?> collection) {
        try {
            return OBJECT_MAPPER.writeValueAsString(collection);
        } catch (JsonProcessingException e) {
            throw new ConversionException("Could not convert collection to json. input = " + collection, e);
        }
    }

    public static String toJsonString(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ConversionException("Could not convert collection to json. input= " + object, e);
        }
    }

    public static <T>List<T> toList(String input, Class<T> type) {
        try {
            CollectionType typeReference =
                    TypeFactory.defaultInstance().constructCollectionType(List.class, type);
            return OBJECT_MAPPER.readValue(input, typeReference);
        } catch (JsonProcessingException e) {
            throw new ConversionException("Could not convert to list of type " + type + ". input=" + input, e);
        }
    }
}