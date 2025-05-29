package com.salescode.dis.insights.sdk;

public enum InsightsEnv {

    LOCAL("http://localhost:8081"),
    DEV("https://dis-insights-uat.salescode.ai"),
    UAT("https://dis-insights-uat.salescode.ai"),
    DEMO("https://dis-insights.salescode.ai"),
    PROD("https://dis-insights.salescode.ai");

    private final String insightsUrl;

    InsightsEnv(String insightsUrl) {
        this.insightsUrl = insightsUrl;
    }

    public String getInsightsUrl() {
        return insightsUrl;
    }
}