package com.salescode.channelkart.event;

import com.salescode.channelkart.event.type.LoggableEvent;
import com.salescode.channelkart.event.type.MutableEvent;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class DefaultEvent<T> implements Event<T>, MutableEvent, Serializable, LoggableEvent {

    private static final long serialVersionUID = -5480406075278418540L;

    private final String lob;

    private final String userName;

    private final EventTopic topic;

    private final T data;

    private String id;

    private int retryCount;

    private int maximumRetry;

    private Map<String, String> loggingAttributes;

    DefaultEvent(String lob, String userName, EventTopic topic, T data) {
        this.lob = lob;
        this.userName = userName;
        this.topic = topic;
        this.data = data;
    }

    @Override
    public String getLob() {
        return this.lob;
    }

    @Override
    public String getUserName() {
        return this.userName;
    }

    @Override
    public EventTopic getTopic() {
        return this.topic;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public T getData() {
        return this.data;
    }

    @Override
    public boolean isRetry() {
        return retryCount > 0;
    }

    @Override
    public int getCurrentRetryCount() {
        return this.retryCount;
    }

    @Override
    public int getMaximumRetry() {
        return maximumRetry;
    }

    @Override
    public boolean hasRetriesLeft() {
        return getCurrentRetryCount() <= maximumRetry;
    }

    public DefaultEvent<T> setMaximumRetry(int maximumRetry) {
        this.maximumRetry = maximumRetry;
        return this;
    }

    public DefaultEvent<T> setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public DefaultEvent<T> withId(String id) {
        setId(id);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultEvent)) return false;

        DefaultEvent<?> that = (DefaultEvent<?>) o;

        if (!lob.equals(that.lob)) return false;
        if (!Objects.equals(userName, that.userName)) return false;
        return topic.equals(that.topic);
    }

    @Override
    public int hashCode() {
        int result = lob.hashCode();
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + topic.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DefaultMessage{" +
                "lob='" + lob + '\'' +
                ", userName='" + userName + '\'' +
                ", topic=" + topic +
                ", data=" + data +
                '}';
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public MutableEvent setMaximumRetryCount(int count) {
        return setMaximumRetry(count);
    }

    @Override
    public Map<String, String> getLoggingAttributes() {
        return this.loggingAttributes == null ? Collections.emptyMap() : loggingAttributes;
    }

    public DefaultEvent<T> setLoggingAttributes(Map<String, String> loggingAttributes) {
        this.loggingAttributes = loggingAttributes;
        return this;
    }
}
