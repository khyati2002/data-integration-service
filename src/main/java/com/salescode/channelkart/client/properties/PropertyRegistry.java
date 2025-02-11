package com.salescode.channelkart.client.properties;


import com.salescode.channelkart.abstractdatasource.AbstractDataSourceConstants;

import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author : Jinu
 * Date    : 8/5/2021
 **/
@Component
public class PropertyRegistry implements com.salescode.channelkart.client.properties.RefreshableRegistry {

    private final com.salescode.channelkart.client.properties.PropertyService service;

    private final Map<String, PropertyHolder> properties = new ConcurrentHashMap<>();

    public PropertyRegistry(PropertyService service) {
        this.service = service;
    }

    public Optional<String> get(String name) {
        return getPropertyHolder()
                .getProperty(name);
    }

    public List<Property> getAll() {
        return getPropertyHolder().getProperties();
    }

    private PropertyHolder getPropertyHolder() {
        String lob = getLob();
        return getPropertyHolder(lob);
    }

    private String getLob() {
        String lob = SecurityContextUtils.getLob();
        if (lob == null) {
            lob = AbstractDataSourceConstants.DEFAULT;
        }
        return lob;
    }

    public String getValue(PropertyDefinition definition) {
        return getOrFail(definition.getName()).getValue();
    }

    public boolean getAsBoolean(PropertyDefinition definition) {
        return Boolean.parseBoolean(getValue(definition));
    }

    public int getAsInteger(PropertyDefinition definition) {
        return Integer.parseInt(getValue(definition));
    }



    public List<String> getAsList(PropertyDefinition definition) {
        String value = getValue(definition);
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    public String getOrDefault(String name, String value) {
        return get(name).orElse(value);
    }

    public Optional<Property> getProperty(String name) {
        return getPropertyHolder().get(name);
    }

    public Property getOrFail(String name) {
        return getProperty(name)
                .orElseThrow(() -> new RuntimeException("Could not find property with name:" + name));
    }

    public void clearRegistry() {
        String lob = SecurityContextUtils.getLob();
        log.debug("Clearing properties for lob:{}", lob);
        if(lob!=null) {
            this.properties.remove(lob);
        }else{
            this.properties.clear();
        }
    }

    private PropertyHolder getPropertyHolder(String lob) {
        return properties.computeIfAbsent(lob, key -> {
            Set<Property> all = service.findAll();
            return new PropertyHolder(all);
        });
    }

    private static class PropertyHolder {

        private final Map<String, Property> properties;

        public PropertyHolder(Set<Property> clientProperties) {
            this.properties = clientProperties.stream()
                    .collect(Collectors.toMap(Property::getName, property -> property, (i, j) -> j));
        }

        public Optional<String> getProperty(String name) {
            Property property = properties.get(name);
            if (property != null) {
                return Optional.of(property.getValue());
            }
            return Optional.empty();
        }

        public Optional<Property> get(String name) {
            return Optional.ofNullable(this.properties.get(name));
        }

        public String getOrDefault(String name, String defaultValue) {
            return getProperty(name).orElse(defaultValue);
        }

        public Map<String, String> toMap() {
            return this.properties.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, p -> p.getValue().getName()));
        }

        public List<Property> getProperties() {
            return new ArrayList<>(this.properties.values());
        }


    }

}
