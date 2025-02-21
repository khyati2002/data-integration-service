/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.utils;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.MapperFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

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
        }
        return OBJECT_MAPPER;
    }

    public static ObjectMapper getObjectMapper() {
        return get();
    }

}