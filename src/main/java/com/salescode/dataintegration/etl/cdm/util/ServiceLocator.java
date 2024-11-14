package com.salescode.dataintegration.etl.cdm.util;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.dataintegration.etl.cdm.CommonDataModelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.utils.CopyOnWriteMap;

import java.io.Serializable;
import java.util.Map;

@Slf4j
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
        log.info("Registering {} service {}", persistentClass.getSimpleName(), abstractCDMService.getClass().getSimpleName());
        SERVICE_REGISTRY.put(persistentClass, abstractCDMService);
    }

}