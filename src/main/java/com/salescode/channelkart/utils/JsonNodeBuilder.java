package com.salescode.channelkart.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.List;

public class JsonNodeBuilder {

    private ObjectNode node;

    public JsonNodeBuilder() {
        this.node = JSONUtils.getObjectMapper().createObjectNode();
    }

    public JsonNodeBuilder with(String key, String value) {
        this.node.put(key, value);
        return this;
    }

    public JsonNodeBuilder with(String key, boolean value) {
        this.node.put(key, value);
        return this;
    }

    public JsonNodeBuilder with(String key, List<String> arrayItems) {
        this.node.set(key, JSONUtils.getObjectMapper().convertValue(arrayItems, JsonNode.class));
        return this;
    }

    public JsonNodeBuilder withIgnoreNull(String key, String value) {
        if (key == null || value == null) {
            // we don't want to add null key or null value
            // it will silently ignore this entry. Use it carefully
            return this;
        }
        this.node.put(key, value);
        return this;
    }

    public JsonNodeBuilder with(String key, Instant instant) {
        this.node.put(key, instant.toString());
        return this;
    }

    public JsonNodeBuilder withIgnoreNull(String key, JsonNode node) {
        if (key == null || node == null) {
            return this;
        }
        this.node.set(key, node);
        return this;
    }

    public ObjectNode build() {
        return node;
    }
}
