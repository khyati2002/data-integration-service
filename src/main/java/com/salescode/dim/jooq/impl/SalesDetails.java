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
public class SalesDetails extends com.salescode.dim.jooq.generated.tables.pojos.SalesDetails implements Serializable {


    private ProductDetails productDetails;
    private String productCode;


    public SalesDetails() {
        super();
    }

    private SalesDetails(SalesDetails salesDetails) {
        super(salesDetails);
    }

    public static SalesDetails of(SalesDetails salesDetails) {
        if (salesDetails == null) {
            return null;
        }
        return new SalesDetails(salesDetails);
    }

    public void setInvoiceNumber(String invoiceNumber) {
        setSaleId(invoiceNumber);
    }
    public String getInvoiceNumber() {
        return getSaleId();
    }
}
