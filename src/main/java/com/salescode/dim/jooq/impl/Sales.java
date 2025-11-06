package com.salescode.dim.jooq.impl;


import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sales extends com.salescode.dim.jooq.generated.tables.pojos.Sales implements Serializable {


    private List<SalesDetails> salesDetails;
    private List<SalesHistory> salesHistory;
    private String invoiceNumber;

    public Sales() {
        super();
    }

    private Sales(Sales sales) {
        super(sales);
    }

    public static Sales of(Sales sales) {
        if (sales == null) {
            return null;
        }
        return new Sales(sales);
    }

}
