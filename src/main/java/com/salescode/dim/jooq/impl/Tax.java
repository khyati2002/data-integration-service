package com.salescode.dim.jooq.impl;

import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tax extends com.salescode.dim.jooq.generated.tables.pojos.Tax implements Serializable {

    public Tax() {
        super();
    }

    private Tax(Tax Tax) {
        super(Tax);
    }

    public static Tax of(Tax Tax) {
        if (Tax == null) {
            return null;
        }
        return new Tax(Tax);
    }
}