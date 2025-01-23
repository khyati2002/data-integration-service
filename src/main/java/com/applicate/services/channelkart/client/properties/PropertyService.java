package com.applicate.services.channelkart.client.properties;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.applicate.services.channelkart.models.MetaData;
import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.utils.JSONUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author : Jinu
 * Date    : 8/5/2021
 **/
@Service
public class PropertyService {

    private static final String DOMAIN_NAME = "client";

    private static final String DOMAIN_TYPE = "properties";

    private final MetaDataService metaDataService;

    public PropertyService(@Lazy MetaDataService metaDataService) {
        this.metaDataService = metaDataService;
    }

    public Set<Property> findAll() {
        Set<Property> storedProperties = findMetaData().map(this::toClientPropertySet).orElseGet(HashSet::new);
        Arrays.stream(PropertyDefinition.values()).forEach(definition -> {
            Property prop = Property.fromDefinition(definition);
            storedProperties.add(prop);
        });
        return storedProperties;
    }

    private Set<Property> toClientPropertySet(MetaData metaData) {
        ArrayNode domainValues = metaData.getDomainValues();
        if (domainValues == null) {
            return Set.of();
        }
        return JSONUtils.stream(domainValues)
                .map(Property::new)
                .collect(Collectors.toSet());
    }

    private Optional<MetaData> findMetaData() {
        return Optional.ofNullable(metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE));
    }
    

}
