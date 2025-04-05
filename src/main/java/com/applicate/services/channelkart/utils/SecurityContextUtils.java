package com.applicate.services.channelkart.utils;

import java.util.Properties;

public class SecurityContextUtils {

    private static Properties properties;

    public static void initialize(Properties properties) {
        if (SecurityContextUtils.properties == null) {
            SecurityContextUtils.properties = properties;
        }
    }

    public static String getPrincipal() {
        return "integration_user";
    }

    public static String getLob() {
        return properties.getProperty("lob");
    }

}
