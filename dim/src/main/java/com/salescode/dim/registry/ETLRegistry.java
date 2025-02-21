package com.salescode.dim.registry;

import com.salescode.dim.interfaces.TypeAwareEtlStep;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import com.salescode.dim.utils.ReflectionUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ETLRegistry implements Serializable {

    private static final long serialVersionUID = 279141430971766979L;

    private static final Map<TypeAwareEtlStep.EtlType, Map<String, TypeAwareEtlStep>> registry = new ConcurrentHashMap<>();
    private static ETLRegistry instance;

    protected ETLRegistry(ExternalRegistryScanner externalRegistryScanner) {
        Collection<TypeAwareEtlStep> instances = externalRegistryScanner.getEtlInstances();
        // Populate the registry with each instance using its class name as key.
        for (TypeAwareEtlStep step : instances) {
            registry.computeIfAbsent(step.getSourceType(), key -> new ConcurrentHashMap<>())
                    .put(step.getClass().getName(), step);
        }
    }

    public static synchronized ETLRegistry getInstance(ExternalRegistryScanner externalRegistryScanner) {
        if (instance == null) {
            instance = new ETLRegistry(externalRegistryScanner);
        }
        return instance;
    }

    public <T> T getTransformer(String fullyQualifiedClassName) {
        return getInstanceByType(TypeAwareEtlStep.EtlType.TRANSFORMER, fullyQualifiedClassName);
    }

    public <T> T getEnrichment(String fullyQualifiedClassName) {
        return getInstanceByType(TypeAwareEtlStep.EtlType.ENRICHMENT, fullyQualifiedClassName);
    }

    public <T> T getValidationRule(String fullyQualifiedClassName) {
        return getInstanceByType(TypeAwareEtlStep.EtlType.VALIDATION, fullyQualifiedClassName);
    }

    @SuppressWarnings("unchecked")
    private <T> T getInstanceByType(TypeAwareEtlStep.EtlType type, String fullyQualifiedClassName) {
        // Retrieve (or create) the inner map for the given type.
        Map<String, TypeAwareEtlStep> typeMap = registry.computeIfAbsent(type, key -> new ConcurrentHashMap<>());
        // Attempt fast O(1) lookup.
        TypeAwareEtlStep instance = typeMap.get(fullyQualifiedClassName);
        if (instance != null) {
            return (T) instance;
        }
        // If not found, try to instantiate and cache the new instance.
        try {
            TypeAwareEtlStep newInstance = ReflectionUtils.createInstance(fullyQualifiedClassName);
            newInstance.open();
            typeMap.put(fullyQualifiedClassName, newInstance);
            return (T) newInstance;
        } catch (Exception e) {
            throw new IllegalArgumentException("No " + type + " found for implementation: " + fullyQualifiedClassName, e);
        }
    }
}