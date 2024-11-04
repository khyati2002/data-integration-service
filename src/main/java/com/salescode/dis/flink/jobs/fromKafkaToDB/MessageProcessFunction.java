package com.salescode.dis.flink.jobs.fromKafkaToDB;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.jooq.impl.DSL;
import org.jooq.impl.TableRecordImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dis.jooq.generated.tables.pojos.CkOrder;
import com.salescode.dis.jooq.generated.tables.records.CkOrderRecord;

import java.time.LocalDateTime;

public class MessageProcessFunction extends KeyedProcessFunction<String, Tuple2<String, ObjectNode>, CkOrder> {

    OutputTag<String> deadLetterTag;

    public MessageProcessFunction(OutputTag<String> deadLetterTag){
        this.deadLetterTag = deadLetterTag;
    }

    //TODO: This needs to be made generic. Collector<CkOrder> out should be Collector<POJOBase> out
    @Override
    public void processElement(Tuple2<String, ObjectNode> tuple, Context context, Collector<CkOrder> out) throws Exception {
        try {
            System.out.println("Tuple key:"+tuple.f0);
            System.out.println("Tuple node:"+tuple.f1);
            // Process the JsonNode here before sinking it
            CkOrder record = processJsonNode(tuple.f1);
            // Emit the processed tuple
            out.collect(record);        
        } catch (Exception e) {
            e.printStackTrace();
            // context.output(deadLetterTag, tuple.f1.asText());
            throw e;
        }
    }

    // Custom method to process the JsonNode before sinking to the database
    private CkOrder processJsonNode(ObjectNode jsonNode) {
        // Add your processing logic here (e.g., modifying fields, filtering, transforming data)
        // For example, modifying a field or adding a new field
        // return jsonNode;

        //Do JSONNode to Record mapping here
        String orderNo = jsonNode.at("/features/0/OrderNo").asText();
        System.out.println("--------"+orderNo);
        CkOrder order = new CkOrder();
        order
            .setOrderno(orderNo)
            .setPrinciplecode("123")
            .setTenantcode("tenant")
            .setCustomercode("cust")
            .setLocationcode("loc")
            .setOrderdate(LocalDateTime.now())
            .setInvoicedate(LocalDateTime.now())
            .setInvoiceno("invno");

        return order;
    }
}