package com.salescode.channelkart.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.salescode.channelkart.utils.NullUtils.requireNonNull;

public class EventBuilder<T> {

    private String lob;

    private String userName;

    private T data;

    private EventTopic topic;

    private int maximumRetriesCount;

    private int currentRetryCount;

    private Map<String, String> loggingAttributes;

    public EventBuilder(Class<T> tClass) {
        // just for support generics
    }

    public EventBuilder() {

    }

    public EventBuilder<T> withLob(String lob) {
        this.lob = lob;
        return this;
    }

    public EventBuilder<T> fromUser(String userName) {
        this.userName = userName;
        return this;
    }

    public EventBuilder<T> withData(T data) {
        this.data = data;
        return this;
    }

    public EventBuilder<T> withTopic(EventTopic topic) {
        this.topic = topic;
        return this;
    }

    public EventBuilder<T> withMaximumRetries(int retryCount) {
        this.maximumRetriesCount = retryCount;
        return this;
    }

    public EventBuilder<T> withCurrentRetryCount(int retryCount) {
        this.currentRetryCount = retryCount;
        return this;
    }

    public EventBuilder<T> withLoggingAttribute(String attributeName, String attributeValue) {
        if (this.loggingAttributes == null) {
            this.loggingAttributes = new HashMap<>();
        }
        this.loggingAttributes.put(attributeName, attributeValue);
        return this;
    }


    public Event<T> build() {
        return buildWithId(UUID.randomUUID().toString());
    }

    public Event<T> buildWithId(String id) {
        requireNonNull(argsInString -> new NullPointerException("Message arguments should not be null:" + argsInString),
                lob, userName, topic, data);
        return new DefaultEvent<>(lob, userName, topic, data)
                .setLoggingAttributes(this.loggingAttributes)
                .withId(id)
                .setMaximumRetry(this.maximumRetriesCount);
    }

    public Event<T> buildRetryEvent(String id) {
        return new DefaultEvent<>(lob, userName, topic, data)
                .setRetryCount(currentRetryCount)
                .setLoggingAttributes(this.loggingAttributes)
                .setMaximumRetry(maximumRetriesCount)
                .withId(id);
    }

}
