package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.enums.OperationType;
import com.applicate.services.channelkart.utils.IdGenerator;
import java.util.List;

public interface CommonDataModelService<T> {

    public List<T> findAll();

    public T save(T cdmObject);

    public T save(T cdmObject, OperationType type);

    public List<T> saveForList(T cdmObject, OperationType type);


    public T findById(String id);

    public void deleteById(String id);

    public void deleteById(String id,boolean failsOnEmptyRecord);

    public List<T> batchSave(Iterable<T> iterObj);

    public List<T> batchSave(Iterable<T> iterObj,IdGenerator idgenerator);

    public T refresh(T cdmObject);

    public List<T> refresh(List<T> cdmobjects);

    public String getKey(T cdmObject);

    void delete(CommonDataModel cdm);
}
