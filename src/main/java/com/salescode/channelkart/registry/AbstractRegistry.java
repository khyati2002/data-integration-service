package com.salescode.channelkart.registry;


import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.services.RegistryLocator;

import java.lang.reflect.ParameterizedType;

public abstract class AbstractRegistry<T extends CommonDataModel> implements Registry<T> {

    public AbstractRegistry() {
        Class<?> persistentClass = (Class<?>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        RegistryLocator.register(persistentClass, this);
    }
}
