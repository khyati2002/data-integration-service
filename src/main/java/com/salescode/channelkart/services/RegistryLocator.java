package com.salescode.channelkart.services;

import com.salescode.channelkart.registry.Registry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class RegistryLocator {

    private static final Map<Class<?>, Registry<?>> registry = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> Registry<T> lookup(Class<T> cdmType) {

        return (Registry<T>) registry.get(cdmType);

    }

    public static <T> void register(Class<T> persistentClass, Registry<?> abstractCDMService) {
        registry.put(persistentClass, abstractCDMService);
    }

    public static Set<Class<?>> getAllTarget() {
        return registry.keySet();
    }

    public static Set<String> getAllTargetName() {
        return getAllTarget().stream().map(r -> r.getSimpleName()).collect(Collectors.toSet());
    }

}
