package com.applicate.services.channelkart.utils;

import java.util.Properties;

public class SecurityContextUtils {

    private static SecurityContextUtils instance;

    private static Properties properties;

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

    public static String getLob() {
        return properties.getProperty("lob");
    }

    public static String getEnv() {
        return properties.getProperty("channelkart.env");
    }

    public static String getToken() {
        return properties.getProperty("channelkart.token");
    }


}
