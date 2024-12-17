package com.salescode.dis.flink.sinks;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import com.salescode.channelkart.models.CommonDataModel;
import org.apache.commons.io.IOExceptionWithCause;
import org.apache.flink.api.connector.sink2.SinkWriter;

public class JPASinkWriter implements SinkWriter<CommonDataModel> {

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

    public JPASinkWriter() throws Throwable {
        try {
            entityManagerFactory = Persistence.createEntityManagerFactory("default");
            entityManager = entityManagerFactory.createEntityManager();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void close() throws Exception {
        if(entityManager != null) {
            entityManager.close();
        }
        if(entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @Override
    public void write(CommonDataModel cdm, Context context) throws java.io.IOException, InterruptedException {
        try {
            System.out.println("About to write the record in DB, entity:"+cdm);
            entityManager.getTransaction().begin();
            entityManager.persist(cdm);
            entityManager.getTransaction().commit();
            System.out.println("Record persisted:"+cdm);
        } catch (Exception e) {
            if(entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            e.printStackTrace();
            throw new IOExceptionWithCause(e);
        }
    }

    @Override
    public void flush(boolean endOfInput) throws java.io.IOException, InterruptedException {
        // No additional flushing behavior required for this sink
    }
}
