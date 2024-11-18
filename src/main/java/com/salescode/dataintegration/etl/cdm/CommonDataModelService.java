package com.salescode.dataintegration.etl.cdm;

import com.salescode.channelkart.models.CommonDataModel;

public interface CommonDataModelService<T extends CommonDataModel> {

    public T refresh(T cdmObject);

    public String getKey(T cdmObject);

    public T save(T cdmObject);

    public T batchSave(Iterable<T> iterObj);
}
