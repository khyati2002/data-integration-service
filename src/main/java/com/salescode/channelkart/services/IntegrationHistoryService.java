//package com.salescode.channelkart.services;
//
//import org.jooq.DSLContext;
//import org.jooq.impl.DSL;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.Date;
//import java.util.UUID;
//
//import static com.salescode.jooq.generated.Tables.CK_INTEGRATION_HISTORY;
//import static com.salescode.jooq.generated.tables.CkOutletDetails.CK_OUTLET_DETAILS;
//
//@Service
//public class IntegrationHistoryService {
//
//    private final DSLContext dsl;
//
//    public IntegrationHistoryService(DSLContext dsl){
//        this.dsl = dsl;
//    }
//    public void save(String result, String description){
//        var record = dsl.newRecord(CK_INTEGRATION_HISTORY);
//        record.set(CK_INTEGRATION_HISTORY.ID, UUID.randomUUID().toString());
//        record.set(CK_INTEGRATION_HISTORY.STATUS, result);
//        record.set(CK_INTEGRATION_HISTORY.DESCRIPTION, description);
//        record.set(CK_INTEGRATION_HISTORY.TIMESTAMP, System.currentTimeMillis());
//        record.set(CK_OUTLET_DETAILS.CREATION_TIME,  Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
//
//        dsl.insertInto(CK_INTEGRATION_HISTORY)
//                .set(record)
//                .execute();
//    }
//
//}
