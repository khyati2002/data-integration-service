package com.salescode.channelkart.services;

import com.salescode.channelkart.models.CommonDataModel;

public interface CommonDataModelService<T extends CommonDataModel> {

    public T refresh(T cdmObject);

    public String getKey(T cdmObject);

    public T save(T cdmObject);


}
