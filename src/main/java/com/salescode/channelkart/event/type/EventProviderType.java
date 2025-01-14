package com.salescode.channelkart.event.type;

import com.salescode.channelkart.event.EventProfile;
import com.salescode.channelkart.event.provider.EmbeddedEventProfile;
import com.salescode.channelkart.event.sqs.SqsEventProfile;

public enum EventProviderType {

    SQS(SqsEventProfile.class), EMBEDDED(EmbeddedEventProfile.class);

    private Class<? extends EventProfile> profileClass;

    EventProviderType(Class<? extends EventProfile> profileClass) {
        this.profileClass = profileClass;
    }

    public static EventProviderType parse(String key) {
        return valueOf(key.toUpperCase());
    }

    public Class<? extends EventProfile> getProfileClass() {
        return this.profileClass;
    }
}
