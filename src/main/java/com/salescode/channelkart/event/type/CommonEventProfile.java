package com.salescode.channelkart.event.type;

import com.salescode.channelkart.event.EventProfile;

public abstract class CommonEventProfile implements EventProfile {

    private EventProviderType providerType;

    private int maximumRetry;

    private int retryDelay;

    private String isDefault;

    @Override
    public EventProviderType getProviderType() {
        return providerType;
    }

    public CommonEventProfile setProviderType(EventProviderType providerType) {
        this.providerType = providerType;
        return this;
    }

    @Override
    public int getMaximumRetry() {
        return maximumRetry;
    }

    public CommonEventProfile setMaximumRetry(int maximumRetry) {
        this.maximumRetry = maximumRetry;
        return this;
    }

    @Override
    public int getRetryDelay() {
        return retryDelay;
    }

    public CommonEventProfile setRetryDelay(int retryDelay) {
        this.retryDelay = retryDelay;
        return this;
    }

    public String getIsDefault() {
        return isDefault;
    }

    public CommonEventProfile setIsDefault(String isDefault) {
        this.isDefault = isDefault;
        return this;
    }

    public boolean isDefault() {
        return "yes".equalsIgnoreCase(this.isDefault);
    }
}
