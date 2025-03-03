package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.CommonDataModel;
import org.jooq.DSLContext;

import java.util.List;

public interface CommonDataModelService<T extends CommonDataModel>{
    public List<T> batchSave(List<T> cdmObject);

    public T save(T cdmObject);

}