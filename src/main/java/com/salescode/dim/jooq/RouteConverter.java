package com.salescode.dim.jooq;

import org.jooq.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RouteConverter implements Converter<String, List<String>> {

    private static final String DELIMITER = ",";

    @Override
    public List<String> from(String databaseValue) {
        if (databaseValue == null || databaseValue.isBlank()) {
            return List.of();
        }
        return Arrays.stream(databaseValue.split(DELIMITER))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    @Override
    public String to(List<String> userObject) {
        if (userObject == null || userObject.isEmpty()) {
            return null;
        }
        return String.join(DELIMITER, userObject);
    }

    @Override
    public Class<String> fromType() {
        return String.class;
    }

    @Override
    public Class<List<String>> toType() {
        return (Class<List<String>>) (Class<?>) List.class;
    }
}
