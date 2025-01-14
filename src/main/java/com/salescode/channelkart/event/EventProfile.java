package com.salescode.channelkart.event;

import com.salescode.channelkart.event.type.EventProviderType;
import com.salescode.channelkart.models.Profile;
import com.salescode.channelkart.utils.JSONUtils;

public interface EventProfile {

    EventProviderType getProviderType();

    int getMaximumRetry();

    int getRetryDelay();

    boolean isDefault();

    static EventProfile createFrom(Profile profile) {
        String providerType = profile.getAttributes().get("providerType").asText();
        EventProviderType type = EventProviderType.parse(providerType);
        return JSONUtils.getObjectMapper().convertValue(profile.getAttributes(), type.getProfileClass());
    }

}
