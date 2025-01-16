/*
 * Copyright (c) 2021. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.models.enums;


import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.applicate.services.channelkart.converters.ActiveStatusDeserializer;
import com.applicate.services.channelkart.converters.ActiveStatusSerializer;
import org.apache.commons.lang3.StringUtils;

/**
 * The enum ActiveStatus.
 *
 * @author Manish Srivastava
 * @version 1.1
 * @since Jan 2021
 */
@JsonSerialize(using = ActiveStatusSerializer.class)
@JsonDeserialize(using = ActiveStatusDeserializer.class)
public enum ActiveStatus {

    /**
     * The active.
     */
    ACTIVE("active"),

    /**
     * The inactive.
     */
    INACTIVE("inactive");

    /**
     * The status.
     */
    private final String status;

    /**
     * Instantiates a new active status.
     *
     * @param status the status
     */
    private ActiveStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the registry.
     *
     * @param str the str
     * @return the registry
     */
    public static ActiveStatus getRegistry(String str) {
        if (StringUtils.isNotBlank(str)) {
            String temp = str.trim();
            if (str.equals("0")) {
                temp = "inactive";
            } else if (str.equals("1")) {
                temp = "active";
            }
            try {
                return ActiveStatus.valueOf(temp.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid active status passed. Only [active,inactive] are allowed");
            }
        }
        return ActiveStatus.INACTIVE;
    }

    /**
     * Gets the status.
     *
     * @return the status
     */
    @JsonValue
    public String getStatus() {
        return this.status;
    }

}
