package com.salescode.dim.jooq;


import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.jooq.impl.SalesInfo;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.Converter;
import org.jooq.JSON;

public class SalesInfoConverter implements Converter<JSON, SalesInfo> {
    private static final ObjectMapper mapper = JSONUtils.getObjectMapper();

    @Override
    public SalesInfo from(JSON databaseObject) {
        if (databaseObject == null) return null;
        try {
            return mapper.readValue(databaseObject.data(), SalesInfo.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse SalesInfo JSON", e);
        }
    }

    @Override
    public JSON to(SalesInfo userObject) {
        if (userObject == null) return null;
        try {
            return JSON.valueOf(mapper.writeValueAsString(userObject));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert SalesInfo to JSON", e);
        }
    }

    @Override
    public Class<JSON> fromType() {
        return JSON.class;
    }

    @Override
    public Class<SalesInfo> toType() {
        return SalesInfo.class;
    }
}

