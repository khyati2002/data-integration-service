package com.salescode.dataintegration.etl.registry;

import com.salescode.dataintegration.etl.interfaces.TypeAwareEtlStep;
import com.salescode.dataintegration.etl.transformer.Transformer;
import com.salescode.dataintegration.scanner.ExternalRegistryScanner;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ETLRegistry {

    private final Map<TypeAwareEtlStep.EtlType, List<TypeAwareEtlStep>> registry;

    public ETLRegistry(ExternalRegistryScanner externalRegistryScanner) {
        Collection<? extends TypeAwareEtlStep> instances = externalRegistryScanner.getInstances();
        registry = instances.stream().collect(Collectors.groupingBy(TypeAwareEtlStep::getSourceType, Collectors.toList()));
    }

    public <T> T getTransformer(String fullyQualifiedClassName) {
        return (T) registry.getOrDefault(TypeAwareEtlStep.EtlType.TRANSFORMER, List.of()).stream()
                .filter(Transformer.class::isInstance)
                .map(Transformer.class::cast)
                .filter(s -> s.getClass().getName().equals(fullyQualifiedClassName))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No transformer found for implementation: " + fullyQualifiedClassName));
    }
}
