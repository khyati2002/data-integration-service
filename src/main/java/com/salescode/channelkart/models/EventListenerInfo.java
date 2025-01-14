package com.salescode.channelkart.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.channelkart.converters.JsonNodeConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "ck_event_listener_info")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventListenerInfo extends CommonDataModel{
    @Column(nullable = false)
    private String topic;

    private String description;

    @Column(nullable = false)
    private String implementation;

    private boolean enabled;

    @Column(columnDefinition = "json")
    @Convert(converter = JsonNodeConverter.class)
    @SuppressWarnings("java:S1948")
    private JsonNode configurations;

    public String getTopic() {
        return topic;
    }

    public EventListenerInfo setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public boolean isEnabled() {
        return getEnabled();
    }

    public boolean getEnabled() {
        return enabled;
    }

    public EventListenerInfo setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public EventListenerInfo setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getImplementation() {
        return implementation;
    }

    public EventListenerInfo setImplementation(String implementation) {
        this.implementation = implementation;
        return this;
    }

    public JsonNode getConfigurations() {
        return configurations;
    }

    public EventListenerInfo setConfigurations(JsonNode configurations) {
        this.configurations = configurations;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EventListenerInfo that = (EventListenerInfo) o;

        if (!Objects.equals(topic, that.topic)) return false;
        if (!Objects.equals(description, that.description)) return false;
        if (!Objects.equals(implementation, that.implementation)) return false;
        return Objects.equals(configurations, that.configurations);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (topic != null ? topic.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (implementation != null ? implementation.hashCode() : 0);
        result = 31 * result + (configurations != null ? configurations.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EventListenerInfo{" +
                "topic='" + topic + '\'' +
                ", description='" + description + '\'' +
                ", implementation='" + implementation + '\'' +
                ", configurations=" + configurations +
                '}';
    }
}
