package com.salescode.dataintegration.etl.registry;

import com.salescode.channelkart.converters.EnrichmentPhase;
import com.salescode.channelkart.utils.ReflectionUtils;
import com.salescode.dataintegration.etl.enrichment.AbstractEnrichment;
import com.salescode.dataintegration.etl.enrichment.Enrichment;
import com.salescode.dataintegration.etl.interfaces.TypeAwareEtlStep;
import com.salescode.dataintegration.etl.transformer.AbstractTransformer;
import com.salescode.dataintegration.etl.transformer.Transformer;
import com.salescode.dataintegration.etl.validation.AbstractValidationRule;
import com.salescode.dataintegration.scanner.ExternalRegistryScanner;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ETLRegistry {

    private final Map<TypeAwareEtlStep.EtlType, List<TypeAwareEtlStep>> registry;

    public ETLRegistry(ExternalRegistryScanner externalRegistryScanner) {
        Collection<? extends TypeAwareEtlStep> instances = externalRegistryScanner.getInstances();
        registry = instances.stream().collect(Collectors.groupingBy(TypeAwareEtlStep::getSourceType, Collectors.toList()));
    }

    public <T> T getTransformer(String fullyQualifiedClassName) {
        Optional<AbstractTransformer> transformer = registry.getOrDefault(TypeAwareEtlStep.EtlType.TRANSFORMER, List.of()).stream()
                .filter(AbstractTransformer.class::isInstance)
                .map(AbstractTransformer.class::cast)
                .filter(s -> s.getClass().getName().equals(fullyQualifiedClassName))
                .findAny();
        if (transformer.isPresent()) {
            return (T) transformer.get();
        }
        try {
            AbstractTransformer newInstance = ReflectionUtils.createInstance(fullyQualifiedClassName);
            registry.computeIfAbsent(TypeAwareEtlStep.EtlType.TRANSFORMER, key -> new ArrayList<>())
                    .add(newInstance);
            return (T) newInstance;
        } catch (Exception e) {
            throw new IllegalArgumentException("No transformer found for implementation: " + fullyQualifiedClassName);
        }
    }


    public <T> T getEnrichment(String fullyQualifiedClassName) {
        Optional<AbstractEnrichment> enrichment = registry.getOrDefault(TypeAwareEtlStep.EtlType.ENRICHMENT, List.of()).stream()
                .filter(AbstractEnrichment.class::isInstance)
                .map(AbstractEnrichment.class::cast)
                .filter(s -> s.getClass().getName().equals(fullyQualifiedClassName))
                .findAny();
        if (enrichment.isPresent()) {
            return (T) enrichment.get();
        }
        try {
            AbstractEnrichment newInstance = ReflectionUtils.createInstance(fullyQualifiedClassName);
            registry.computeIfAbsent(TypeAwareEtlStep.EtlType.ENRICHMENT, key -> new ArrayList<>())
                    .add(newInstance);
            return (T) newInstance;
        } catch (Exception e) {
            throw new IllegalArgumentException("No Enrichment found for implementation: " + fullyQualifiedClassName);
        }
    }

    public <T> T getValidationRule(String fullyQualifiedClassName) {
        Optional<AbstractValidationRule> validationRule = registry.getOrDefault(TypeAwareEtlStep.EtlType.VALIDATION, List.of()).stream()
                .filter(AbstractValidationRule.class::isInstance)
                .map(AbstractValidationRule.class::cast)
                .filter(s -> s.getClass().getName().equals(fullyQualifiedClassName))
                .findAny();
        if (validationRule.isPresent()) {
            return (T) validationRule.get();
        }
        try {
            AbstractValidationRule newInstance = ReflectionUtils.createInstance(fullyQualifiedClassName);
            registry.computeIfAbsent(TypeAwareEtlStep.EtlType.VALIDATION, key -> new ArrayList<>())
                    .add(newInstance);
            return (T) newInstance;
        } catch (Exception e) {
            throw  new IllegalArgumentException("No Validation Rule found for implementation: " + fullyQualifiedClassName);
        }
    }

}
