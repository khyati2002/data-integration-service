package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.CommonDataModel;

import java.util.Collection;

public interface CommonDataModelService<T extends CommonDataModel> {

    Collection<T> batchSave(Collection<T> cdmObject);

    T save(T cdmObject);

    Class<T> getPersistentClass();

}