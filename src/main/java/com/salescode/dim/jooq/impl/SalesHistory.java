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
public class SalesHistory extends com.salescode.dim.jooq.generated.tables.pojos.SalesHistory implements Serializable {

    private String invoiceNumber;

    public SalesHistory() {
        super();
    }

    private SalesHistory(SalesHistory salesHistory) {
        super(salesHistory);
    }

    public static SalesHistory of(SalesHistory salesHistory) {
        if (salesHistory == null) {
            return null;
        }
        return new SalesHistory(salesHistory);
    }


}
