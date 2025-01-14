package com.salescode.channelkart.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.salescode.channelkart.services.enums.EntityOperation;
import com.salescode.channelkart.utils.CompressionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Set;
import java.util.UUID;
import java.util.zip.DataFormatException;

@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.CLASS)
public class StreamingEventData<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingEventData.class);
    private String requestId = UUID.randomUUID().toString();
    private byte[] object;
    private EntityOperation operation;
    private String clazz;
    private String lob;
    private String loginId;
    private Set<String> eventTopics;

    private transient Object model;


    public StreamingEventData(T object, EntityOperation operation) {
        this(object);
        this.operation = operation;
    }

    public StreamingEventData(T object) {
        this.object = serialize(object);
        this.clazz = object.getClass().getName();
        this.operation = null;
    }

    public StreamingEventData() {
    }

    private byte[] serialize(Object cdm) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] objectInBytes;
        try(ObjectOutputStream out = new ObjectOutputStream(bos)){
            out.writeObject(cdm);
            out.flush();
            objectInBytes = CompressionUtils.compress(bos.toByteArray()); //Compress with ZLIB
        } catch (IOException e) {
            LOGGER.error("Unable serialize  object of type {} | {} | {}", cdm.getClass().getSimpleName(), cdm, e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                LOGGER.error("Unable to close byte stream object of type {} | {} | {}", cdm.getClass().getSimpleName(), cdm, ex.getMessage());
            }
        }
        return objectInBytes;
    }

    public Object retreiveObject() {
        if (model != null) {
            return model;
        }
        byte[] obj;
        try {
            obj = CompressionUtils.decompress(object);
        } catch (IOException | DataFormatException e) {
            LOGGER.info("Request Id {} is in old data formats", requestId);
            obj = object;
        }
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(obj);
            ObjectInput in = new ObjectInputStream(bis);
            this.model = in.readObject();
            return model;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            LOGGER.error("Unable to deserialise obj {}" , e.getMessage());
        }
        return model;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public byte[] getObject() {
        return object;
    }

    public void setObject(byte[] object) {
        this.object = object;
    }

    public EntityOperation getOperation() {
        return operation;
    }

    public void setOperation(EntityOperation operation) {
        this.operation = operation;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getLob() {
        return lob;
    }

    public void setLob(String lob) {
        this.lob = lob;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public void setEventTopics(Set<String> eventTopics) {
        this.eventTopics = eventTopics;
    }

    public Set<String> getEventTopics() {
        return eventTopics;
    }
}

