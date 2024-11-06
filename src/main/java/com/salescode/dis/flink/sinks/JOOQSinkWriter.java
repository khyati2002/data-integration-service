package com.salescode.dis.flink.sinks;

import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.commons.io.IOExceptionWithCause;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import com.salescode.dis.jooq.generated.tables.pojos.CkOrder;
import com.salescode.dis.jooq.generated.tables.records.CkOrderRecord;


//TODO: Change CkOrder to common base class that all POJOs implement
public class JOOQSinkWriter implements SinkWriter<CkOrder> {
    
    private DSLContext dslContext;
    private Connection connection;

    public JOOQSinkWriter(String url, String user, String password) throws Throwable {
        try {
            System.out.println(url+","+user+","+password);
            System.out.println("--------------------");
            //For some reason flink needs below nudge to load jdbc driver.
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, password);
            dslContext = DSL.using(connection, SQLDialect.MYSQL, new Settings());

        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
            //throw some exception here and handle it.
        }
    }


    @Override
    public void close() throws Exception {
        if(connection != null){
            connection.close();
        }
    }

    @Override
    public void write(CkOrder order, Context context) throws java.io.IOException, InterruptedException {
        try {
            System.out.println("About to write the record in DB, pojo:"+order);

            org.jooq.Record record = new CkOrderRecord(order);

            System.out.println("Record:"+record);
            System.out.println("*****************");
            System.out.println("Printing DSLContext"+dslContext.configuration().connectionProvider().acquire().getMetaData().getURL());
            System.out.println(dslContext);

            record.attach(dslContext.configuration());
            dslContext.insertInto(DSL.table("ck_order"))
            .set(record)
            .execute();
        } catch (Exception e) {
            // throw new IOException("Failed to write record", e);
            e.printStackTrace();
            throw new IOExceptionWithCause(e);
        }

    }


    @Override
    public void flush(boolean endOfInput) throws java.io.IOException, InterruptedException {
        // No additional flushing behavior required for this sink
    }
}