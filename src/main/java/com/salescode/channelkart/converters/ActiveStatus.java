/*
 * Copyright (c) 2021. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.converters;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.StringUtils;

@JsonSerialize(using = ActiveStatusSerializer.class)
@JsonDeserialize(using = ActiveStatusDeserializer.class)
public enum ActiveStatus {

    ACTIVE("active"),

    INACTIVE("inactive");

    private final String status;

    private ActiveStatus(String status) {
        this.status = status;
    }

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
//	   AuditLogger.log("Active Status","Invalid active status passed in input parameter. Found : "+str+", Only [active,inactive] are allowed",AuditLogger.Status.FAILURE,"getRegistery",AuditLogger.Operations.GET.toString(),null);
        return ActiveStatus.INACTIVE;
    }

    @JsonValue
    public String getStatus() {
        return this.status;
    }

}
