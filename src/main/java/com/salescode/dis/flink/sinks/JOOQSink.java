package com.salescode.dis.flink.sinks;

import java.io.IOException;

import com.salescode.channelkart.models.CommonDataModel;
import org.apache.commons.io.IOExceptionWithCause;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;

//TODO: Change CkOrder to common base class that all POJOs implement. We cannot use jooq record class becasue it's not serializable.
//TODO: Sink and SinkWriter cannot be Autowired, flinks failes to distribute them if they're autowired
public class JOOQSink implements Sink<CommonDataModel> {

    private String url;

    private String user;
    
    private String password;

    public JOOQSink(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @Override
    public SinkWriter<CommonDataModel> createWriter(InitContext context) throws IOException{
        try {
            System.out.println("*****************");
            System.out.println(url);            
            return new JOOQSinkWriter(url, user, password);
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new IOExceptionWithCause(e);
        }
    }
}