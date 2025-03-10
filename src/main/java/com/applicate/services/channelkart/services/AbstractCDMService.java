package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.CommonDataModel;
import lombok.Getter;
import lombok.Setter;
import org.jooq.DSLContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class AbstractCDMService<T extends CommonDataModel> implements CommonDataModelService<T>, DatabaseAwareService {

    @Getter
    private Class<T> persistentClass;

    @Getter
    @Setter
    private DSLContext dslContext;

    public AbstractCDMService() {
        if (getClass().getGenericSuperclass() instanceof ParameterizedType) {
            Type genericSuperclass = getClass().getGenericSuperclass();
            ParameterizedType paramType = (ParameterizedType) genericSuperclass;
            this.persistentClass = (Class<T>) paramType.getActualTypeArguments()[0];
        }
    }

}
