package com.salescode.dim.jooq.impl;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSetter;

import java.io.Serializable;

public class DeliveryPJP extends com.salescode.dim.jooq.generated.tables.pojos.DeliveryPjp implements Serializable {

    private DeliveryPJP(){

    }
    private DeliveryPJP(com.salescode.dim.jooq.generated.tables.pojos.DeliveryPjp deliveryPjp) {
        super(deliveryPjp);
    }
    public static DeliveryPJP of(com.salescode.dim.jooq.generated.tables.pojos.DeliveryPjp deliveryPjp) {
        if(deliveryPjp == null) {
            return null;
        }
        return new DeliveryPJP(deliveryPjp);
    }

    @JsonSetter("loginId")
    public void setLoginId(String loginId) {
        setLoginid(loginId);
    }

}
