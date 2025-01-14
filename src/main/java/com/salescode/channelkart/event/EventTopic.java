package com.salescode.channelkart.event;

import com.salescode.channelkart.utils.NullUtils;

import java.io.Serializable;

public class EventTopic implements Serializable {

    private static final long serialVersionUID = -8450585402118699053L;

    private String topic;

    private EventTopic(String topic) {
        this.topic = topic;
    }

    public String value() {
        return this.topic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EventTopic that = (EventTopic) o;

        return topic.equals(that.topic);
    }

    @Override
    public int hashCode() {
        return topic.hashCode();
    }

    public static EventTopic of(String topic) {
        NullUtils.requireNonNull(() -> new NullPointerException("topic name should not be null"), topic);
        return new EventTopic(topic);
    }

    @Override
    public String toString() {
        return "MessageTopic[" + topic + "]";
    }
}

