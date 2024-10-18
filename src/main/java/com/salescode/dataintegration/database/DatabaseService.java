package com.salescode.dataintegration.database;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import java.util.concurrent.atomic.AtomicInteger;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class DatabaseService {

    AtomicInteger counter = new AtomicInteger(0);

    @Autowired
    private DSLContext dslContext;

    @CircuitBreaker(name = "dbInsert")
    @Retry(name = "dbInsert")
    public void dbInsert(String message) {
        // try {
        //     // Assume message contains data to map into POJO or directly in query
        //     dslContext.insertInto(DSL.table("your_table"))
        //             .set(DSL.field("column_name"), message)
        //             .execute();
        //     return true;
        // } catch (Exception e) {
        //     // Log the error
        //     return false;
        // }
        // if(counter.incrementAndGet() %2 == 0){
        //     System.out.println("Message Processed: "+message);            
        // }
        // else{
        //     System.out.println("Message will not be processed: "+message);
        //     throw new RuntimeException("Odd counter!!!");
        // }
        System.out.println("Message Processed: "+message);            
    }
}
