package com.salescode.dataintegration.etl.cdm.util;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.dataintegration.etl.cdm.CommonDataModelService;
import org.apache.kafka.common.utils.CopyOnWriteMap;

import java.util.Map;

public class ServiceLocator {

    private static final Map<Class<?>, CommonDataModelService<?>> SERVICE_REGISTRY = new CopyOnWriteMap<>();

    private ServiceLocator() {
        throw new UnsupportedOperationException("Utility Class");
    }

    public static <T extends CommonDataModel> CommonDataModelService<T> lookup(Class<T> cdmType) {
        return (CommonDataModelService<T>) SERVICE_REGISTRY.get(cdmType);
    }

    public static <T> void register(Class<T> persistentClass, CommonDataModelService<?> abstractCDMService) {
        SERVICE_REGISTRY.put(persistentClass, abstractCDMService);
    }

}