package com.salescode.channelkart.event;

import com.salescode.channelkart.models.EventListenerInfo;
import com.salescode.channelkart.utils.ReflectionUtils;
import org.apache.flink.runtime.util.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Component
public class EventListeners {

    private static final Logger log = LoggerFactory.getLogger(EventListeners.class);

    private final ConcurrentMap<String, EventListener<?>> topicListeners;

    public EventListeners(List<EventListener<?>> listeners) {
        this.topicListeners = toMap(listeners);
        log.info("Initialized listeners:{}", listeners);
    }

    private ConcurrentMap<String, EventListener<?>> toMap(List<EventListener<?>> listeners) {
        return listeners.stream()
                .collect(Collectors.toConcurrentMap(listener -> ClassUtils.getUserClass(listener).getName(), listener -> listener));
    }

    @SuppressWarnings("unchecked")
    public <T> EventListener<T> get(String listenerName) {
        return (EventListener<T>) topicListeners.computeIfAbsent(listenerName, this::findAndCreateListener);
    }

    public <T> EventListener<T> get(EventListenerInfo info) {
        return get(info.getImplementation());
    }

    private <T> EventListener<T> findAndCreateListener(String name) {
        return ReflectionUtils.createInstance(name);
    }
}

