package com.applicate.services.channelkart.repository;


import org.jooq.DSLContext;

public class RouteInfoRepository  {
    private final DSLContext dsl;

    public RouteInfoRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

}