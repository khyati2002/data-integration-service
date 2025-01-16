package com.applicate.services.channelkart.registry;


import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.RegistryLocator;

import java.lang.reflect.ParameterizedType;

public abstract class AbstractRegistry<T extends CommonDataModel> implements Registry<T> {

    public AbstractRegistry() {
        Class<?> persistentClass = (Class<?>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        RegistryLocator.register(persistentClass, this);
    }
}
