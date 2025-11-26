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
public class Stock extends com.salescode.dim.jooq.generated.tables.pojos.Stock implements Serializable {

    public Stock() {
        super();
    }

    private Stock(Stock stock) {
        super(stock);
    }

    public static Stock of(Stock stock) {
        if (stock == null) {
            return null;
        }
        return new Stock(stock);
    }

}