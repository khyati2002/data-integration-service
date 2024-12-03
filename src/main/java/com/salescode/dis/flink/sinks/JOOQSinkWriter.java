package com.salescode.dis.flink.sinks;

import java.sql.Connection;
import java.sql.DriverManager;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.services.CommonDataModelService;
import com.salescode.channelkart.services.ServiceLocator;
import org.apache.commons.io.IOExceptionWithCause;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;



//TODO: Change CkOrder to common base class that all POJOs implement
public class JOOQSinkWriter implements SinkWriter<CommonDataModel> {

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
    public void write(CommonDataModel cdm, Context context) throws java.io.IOException, InterruptedException {
        try {
            System.out.println("About to write the record in DB, pojo:"+cdm);
            CommonDataModelService lookup = ServiceLocator.lookup(cdm.getClass());
            lookup.save(cdm);
            System.out.println("Record:"+cdm);
            System.out.println("*****************");
            System.out.println("Printing DSLContext"+dslContext.configuration().connectionProvider().acquire().getMetaData().getURL());
            System.out.println(dslContext);


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