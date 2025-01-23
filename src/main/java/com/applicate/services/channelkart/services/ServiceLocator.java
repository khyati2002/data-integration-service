package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.scanner.BundleResource;
import com.applicate.services.channelkart.scanner.ExternalRegistryScanner;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceLocator {

    private static final Map<Class<?>, CommonDataModelService<?>> SERVICE_REGISTRY = new ConcurrentHashMap<>();

    private ServiceLocator() {
        throw new UnsupportedOperationException("Utility Class");
    }

    @SuppressWarnings("unchecked")
    public static <T> CommonDataModelService<T> lookup(Class<T> cdmType) {
        return (CommonDataModelService<T>) SERVICE_REGISTRY.get(cdmType);
    }

    public static <T> void register(Class<T> persistentClass, CommonDataModelService<?> abstractCDMService) {
        SERVICE_REGISTRY.put(persistentClass, abstractCDMService);
    }

    private static boolean isTaskType(BundleResource resource) {
        return "task".equalsIgnoreCase(resource.getType());
    }

}
