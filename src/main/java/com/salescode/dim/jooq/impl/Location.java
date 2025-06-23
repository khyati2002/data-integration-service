package com.salescode.dim.jooq.impl;

import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Location extends com.salescode.dim.jooq.generated.tables.pojos.Location implements Serializable {

    public Location(){
        super();
    }

    public Location(com.salescode.dim.jooq.generated.tables.pojos.Location location) {
        super(location);
    }

    public static Location of(com.salescode.dim.jooq.generated.tables.pojos.Location location) {
        return new Location(location);
    }

}
