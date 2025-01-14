package com.salescode.channelkart.event.provider;

import com.salescode.channelkart.event.EventProducer;
import com.salescode.channelkart.event.EventProfile;
import com.salescode.channelkart.event.embedded.EmbeddedEventProvider;
import com.salescode.channelkart.event.sqs.SqsEventProfile;
import com.salescode.channelkart.event.sqs.SqsEventProvider;
import com.salescode.channelkart.event.type.EventProviderType;

public interface EventProvider {

    void shutDown();

    EventProducer getProducer();

    EventProfile getProfile();

    boolean isSameProfile(EventProfile profile);

    static EventProvider create(EventProfile profile) {
        EventProviderType providerType = profile.getProviderType();
        if (providerType == EventProviderType.SQS) {
            return new SqsEventProvider((SqsEventProfile) profile);
        }
        if (providerType == EventProviderType.EMBEDDED) {
            return new EmbeddedEventProvider((EmbeddedEventProfile) profile);
        }
        throw new IllegalArgumentException("Could not find valid event provider for profile :" + profile);
    }
}
