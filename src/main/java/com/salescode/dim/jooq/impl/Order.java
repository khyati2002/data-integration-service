package com.salescode.dim.jooq.impl;


import com.salescode.dim.jooq.generated.tables.pojos.Orders;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;


@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Order extends com.salescode.dim.jooq.generated.tables.pojos.Orders implements Serializable {

    public Order() {
        super();
    }

    private Order(Orders order) {
        super(order);
    }

    public static Order of(Orders order) {
        if (order == null) {
            return null;
        }
        return new Order(order);
    }

}