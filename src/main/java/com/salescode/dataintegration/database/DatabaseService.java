package com.salescode.dataintegration.database;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class DatabaseService {
    @Autowired
    private DSLContext dsl;

    @CircuitBreaker(name = "dbInsert", fallbackMethod = "fallbackInsert")
    @Retry(name = "dbInsert")
    public void insertData(String message) {
        dsl.insertInto(DSL.table("your_table"))
           .set(DSL.field("message"), message)
           .execute();
    }

    public void fallbackInsert(String message, Throwable t) {
        // Fallback logic for failed inserts
    }
}
