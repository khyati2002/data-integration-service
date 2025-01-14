package com.salescode.channelkart.event.type;

public interface MutableEvent {

    void setId(String id);

    MutableEvent setRetryCount(int count);

    MutableEvent setMaximumRetryCount(int count);

}
