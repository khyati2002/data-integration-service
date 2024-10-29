package com.salescode.dataintegration.etl.interfaces;

public interface CommonDataModelService<T> {

    public T refresh(T cdmObject);

    public String getKey(T cdmObject);

}
