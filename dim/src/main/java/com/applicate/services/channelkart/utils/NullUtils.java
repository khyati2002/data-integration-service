package com.applicate.services.channelkart.utils;

public class NullUtils {

    private NullUtils() {
    }

    /**
     * Checks for null values.
     *
     * @param args the args
     * @return true, if successful
     */
    public static boolean hasNullValues(Object... args) {
        for (Object item : args) {
            if (item == null) {
                return true;
            }
        }
        return false;
    }
}