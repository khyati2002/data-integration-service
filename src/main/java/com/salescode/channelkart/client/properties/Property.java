package com.salescode.channelkart.client.properties;


import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author : Jinu
 * Date    : 8/5/2021
 **/
public class Property {

    private String name;

    private String value;

    private String description;

    private String defaultValue;

    private List<String> supportedValues;

    //type can be string,boolean,integer,float
    private String type;

    public Property() {}


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Property(JsonNode node) {
        this(node.get("name").asText(), node.get("value").asText(),
            node.has("description")? node.get("description").asText():null,node.has("defaultValue")? node.get("defaultValue").asText():null,
            node.has("type")? node.get("type").asText():"string",node.has("supportedValues")?node.get("supportedValues"):null);
    }

    public Property(String name, String value, String description, String defaultValue, String type, JsonNode supportedValuesNode) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        this.name = name;
        this.value = value;
        this.defaultValue=defaultValue;
        this.description=description;
        this.type=type;
        this.supportedValues=supportedValuesNode!=null?toSupportedValues(supportedValuesNode):null;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public Property setName(String name) {
        this.name = name;
        return this;
    }

    public Property setValue(String value) {
        this.value = value;
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Property property = (Property) o;
        return Objects.equals(name, property.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }


    public static Property fromDefinition(PropertyDefinition definition) {
        return new Property(definition.getName(), definition.getDefaultValue(),definition.getDescription(),definition.getDefaultValue(),"string",null);
    }

    private List<String> toSupportedValues(JsonNode valuesNode){
        List<String> values = new ArrayList<>();
        for (JsonNode node : valuesNode) {
            values.add(node.asText());
        }
        return values;
    }
}
