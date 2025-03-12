package com.applicate.services.channelkart.utils;

import java.util.Properties;

public class SecurityContextUtils {

    private static SecurityContextUtils instance;

    private Properties properties;

    public SecurityContextUtils(Properties properties) {
        this.properties = properties;
    }

    public static SecurityContextUtils getInstance() {
        if (instance == null) {
            throw new AssertionError("Cannot instantiate utility class");
        }
        return instance;
    }


    public static SecurityContextUtils getInstance(Properties properties) {
        if (instance == null) {
            return new SecurityContextUtils(properties);
        }
        return instance;
    }

    public static String getPrincipal() {
        return "integration_user";
    }

    public String getLob() {
        return properties.getProperty("lob");
    }


}
