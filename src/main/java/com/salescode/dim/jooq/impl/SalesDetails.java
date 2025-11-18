package com.salescode.dim.jooq.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SalesDetails extends com.salescode.dim.jooq.generated.tables.pojos.SalesDetails implements Serializable {

    public SalesDetails() {
        super();
    }

    public SalesDetails(com.salescode.dim.jooq.generated.tables.pojos.SalesDetails details) {
        super(details);
    }

    public static SalesDetails of(com.salescode.dim.jooq.generated.tables.pojos.SalesDetails details) {
        if (details == null) {
            return null;
        }
        return new SalesDetails(details);
    }
}

