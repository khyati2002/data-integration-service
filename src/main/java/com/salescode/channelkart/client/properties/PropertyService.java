package com.salescode.channelkart.client.properties;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.channelkart.models.MetaData;
import com.salescode.channelkart.services.MetaDataService;
import com.salescode.channelkart.utils.JSONUtils;
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

    public PropertyService(MetaDataService metaDataService) {
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

    private MetaData newMetaData() {
        MetaData metaData = new MetaData();
        metaData.setDescription("Client specific properties");
        metaData.setDomainName(DOMAIN_NAME);
        metaData.setDomainType(DOMAIN_TYPE);
        metaData.setDomainValues(JSONUtils.getObjectMapper().createArrayNode());
        return metaData;
    }

    public Property createOrUpdate(Property clientProperty) {
        MetaData metaData = findMetaData().orElseGet(this::newMetaData);
        ArrayNode domainValues = metaData.getDomainValues();
        Set<Property> filteredProperties = JSONUtils.stream(domainValues)
                .map(Property::new)
                //Get all the properties except the current property to update
                .filter(property -> !property.getName().equals(clientProperty.getName()))
                .collect(Collectors.toSet());
        // add the updated property
        filteredProperties.add(clientProperty);
        ArrayNode updatedDomainValues = filteredProperties.stream()
                .map(Property::toNode)
                .collect(JSONUtils.toArrayNode());
        metaData.setDomainValues(updatedDomainValues);
        metaDataService.save(metaData);
        return clientProperty;
    }

    public void deleteProperty(String name) {
        MetaData metaData = findMetaData().orElseGet(this::newMetaData);
        ArrayNode domainValues = metaData.getDomainValues();
        ArrayNode filteredDomainValues = JSONUtils.stream(domainValues)
                .filter(node -> !node.get("name").asText().equals(name))
                .collect(JSONUtils.toArrayNode());
        metaData.setDomainValues(filteredDomainValues);
        metaDataService.save(metaData);
    }
}
