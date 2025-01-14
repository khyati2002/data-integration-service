package com.salescode.channelkart.event.queue.message;

public interface SerializableEventData {
    String serialize() ;
    void deserialize(String payload);
}
