package com.salescode.dataintegration.etl.metadata.registry;

import com.salescode.channelkart.event.EventProducer;
import com.salescode.channelkart.security.SecurityContextUtils;
import org.springframework.stereotype.Component;

@Component
public class EventProducerFactory {

    private final EventProviderRegistry eventProviderRegistry;

    public EventProducerFactory(EventProviderRegistry eventProviderRegistry) {
        this.eventProviderRegistry = eventProviderRegistry;
    }

    public EventProducer get(String lob, String profileName) {
        return this.eventProviderRegistry.get(lob, profileName).getProducer();
    }

    public EventProducer getByName(String profileName) {
        String lob = SecurityContextUtils.getLob();
        return get(lob, profileName);
    }

    public EventProducer getDefault(String lob) {
        return this.eventProviderRegistry.getDefault(lob).getProducer();
    }

    public EventProducer getDefault() {
        String lob = SecurityContextUtils.getLob();
        return getDefault(lob);
    }

}

