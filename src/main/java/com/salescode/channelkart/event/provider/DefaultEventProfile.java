package com.salescode.channelkart.event.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.salescode.channelkart.models.Profile;
import com.salescode.channelkart.utils.JSONUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultEventProfile {

    public static final String EVENT_PROVIDER_PROFILE = "eventProvider";

    @Value("${channelkart.default.event.profile}")
    private String defaultProfile;

    public Profile getProfile() throws JsonProcessingException {
        Profile profile = new Profile();
        profile.setName("default");
        profile.setType(EVENT_PROVIDER_PROFILE);
        profile.setAttributes(JSONUtils.getObjectMapper().readTree(this.defaultProfile));
        return profile;
    }



}
