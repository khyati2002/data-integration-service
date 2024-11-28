package com.salescode.channelkart.utils;

public class SecurityContextUtils {

    public static final String INTEGRATION_USER = "integration_user";

    private static String staticLob;

    public static String getPrincipal() {
        return INTEGRATION_USER;
    }

    public static String getLob() {
        return staticLob;
    }

    public static void setLob(String lob) {
        SecurityContextUtils.staticLob = lob;
    }
}