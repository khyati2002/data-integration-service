package com.salescode.channelkart.event;

public interface Event<T> {

    String getLob();

    String getUserName();
    EventTopic getTopic();

    T getData();

    String getId();

    public boolean isRetry();

    public int getCurrentRetryCount();

    public int getMaximumRetry();

    public boolean hasRetriesLeft();
    static <T> EventBuilder<T> builder() {
        return new EventBuilder<>();
    }
}
