package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.CommonDataModel;
import org.apache.kafka.common.utils.CopyOnWriteMap;

import java.util.Map;

public class ServiceLocator {
    private static final Map<Class<?>, CommonDataModelService<?>> SERVICE_REGISTRY = new CopyOnWriteMap<>();

    private ServiceLocator() {
        throw new UnsupportedOperationException("Utility Class");
    }

    public static <T extends CommonDataModel> CommonDataModelService<T> lookup(Class<T> cdmType) {
        CommonDataModelService<?> commonDataModelService = SERVICE_REGISTRY.get(cdmType);
        return (CommonDataModelService<T>) commonDataModelService;
    }

    public static <T> void register(Class<T> persistentClass, CommonDataModelService<?> abstractCDMService) {
      //  log.info("Registering {} service {}", persistentClass.getSimpleName(), abstractCDMService.getClass().getSimpleName());
        SERVICE_REGISTRY.put(persistentClass, abstractCDMService);
    }

}
