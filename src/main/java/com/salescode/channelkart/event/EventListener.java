package com.salescode.channelkart.event;

import com.salescode.channelkart.models.EventListenerInfo;

public interface EventListener<T>{
    void listen(Event<T> event);

    default void listen(Event<T> event, EventListenerInfo info) {
        listen(event);
    }
}
