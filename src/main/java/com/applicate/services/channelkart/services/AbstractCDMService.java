package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.jooq.impl.OutletDetails;
import lombok.Getter;
import lombok.Setter;
import org.jooq.DSLContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public Collection<T> batchSave(Collection<T> cdmObject) {
        return cdmObject.stream().map(this::save).collect(Collectors.toList());
    }

    public <T extends CommonDataModel> T addHash(T model) {
        String hash = model.hash();
        model.setHash(hash);
        return model;
    }

    @Override
    public T save(T cdmObject) {
        return cdmObject;
    }

}
