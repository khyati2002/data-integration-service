package com.applicate.services.channelkart.services;

import org.jooq.Record;
import org.jooq.Result;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A service to execute raw SQL queries using the project's shared jOOQ DSLContext.
 * This is a replacement for the old Spring-based JdbcTemplate service.
 */
public class QueryService {

    /**
     * Executes a raw SQL query that returns a list of results.
     * * @param query The raw SQL string to execute.
     * @return A List of Maps, where each map represents a row.
     */
    public List<Map<String, Object>> execute(String query) {
        Result<Record> result = AbstractCDMService.getDslContext().resultQuery(query).fetch();

        // Convert the jOOQ Result into the List<Map<String, Object>> format
        return result.stream()
                .map(Record::intoMap)
                .collect(Collectors.toList());
    }

    /**
     * Executes a raw SQL query with parameters.
     * * @param query The raw SQL string with '?' placeholders.
     * @param args  The arguments to bind to the placeholders.
     * @return A List of Maps, where each map represents a row.
     */
    public List<Map<String, Object>> execute(String query, Object... args) {
        // Get the shared DSLContext and bind parameters
        Result<Record> result = AbstractCDMService.getDslContext().resultQuery(query, args).fetch();

        // Convert the jOOQ Result into the List<Map<String, Object>> format
        return result.stream()
                .map(Record::intoMap)
                .collect(Collectors.toList());
    }
}