package com.salescode.channelkart.services.enums;

public enum EntityOperation {
    INSERT, DELETE, UPDATE, RELOAD;

    public static EntityOperation parse(String operation) {
        return EntityOperation.valueOf(operation.toUpperCase());
    }
}
