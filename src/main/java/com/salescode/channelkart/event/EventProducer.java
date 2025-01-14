package com.salescode.channelkart.event;

import software.amazon.awssdk.utils.async.StoringSubscriber;

public interface EventProducer {

    /**
     *
     * @param event
     * @param <T>
     * @return the corresponding  id
     */
    <T> String  send(Event<T> event);
}
