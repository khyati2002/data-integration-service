package com.salescode.dataintegration.etl.cdm;

import com.salescode.channelkart.models.CommonDataModel;

import java.util.List;
import java.util.Map;

public interface CommonDataModelService<T extends CommonDataModel> {

    public T refresh(T cdmObject);

    public String getKey(T cdmObject);

    public T save(T cdmObject);

     public List<T> batchSave(Iterable<T> iterObj);

     public List<T> batchSave(Map<String,T> iterObj);

}
