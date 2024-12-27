package com.salescode.channelkart.services;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.services.enums.OperationType;

import java.util.List;

public interface CommonDataModelService<T extends CommonDataModel> {

    public T refresh(T cdmObject);

    public String getKey(T cdmObject);

    public T save(T cdmObject);

    public T populateData(T cdmObject);

    public List<T> saveForList(T cdmObject, OperationType type);

    public void deleteById(String id,boolean failsOnEmptyRecord);

}
