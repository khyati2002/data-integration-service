package com.salescode.jooq;

import org.jooq.Converter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class DateConverter implements Converter<LocalDateTime, Date> {

    @Override
    public Date from(LocalDateTime databaseObject) {
        // Convert Date to String formatted to client timezone
        return databaseObject != null ? Date.from(databaseObject.toInstant(ZoneOffset.UTC)) : null;
    }

    @Override
    public LocalDateTime to(Date userObject) {
        // Convert formatted String back to Date
        try {
            return userObject != null ? LocalDateTime.ofInstant(userObject.toInstant(), ZoneOffset.UTC) : null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse date: " + userObject, e);
        }
    }

    @Override
    public Class<LocalDateTime> fromType() {
        return LocalDateTime.class;
    }

    @Override
    public Class<Date> toType() {
        return Date.class;
    }
}