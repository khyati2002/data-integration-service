package com.salescode.dim.jooq.impl;


import com.applicate.services.channelkart.utils.JSONUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.beans.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sales extends com.salescode.dim.jooq.generated.tables.pojos.Sales implements Serializable {


    private List<SalesDetails> salesDetails;
    private List<SalesHistory> salesHistory;
    private String invoiceNumber;
    private boolean update;
    private Boolean outletExists;

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

    public String getOutletCode() {
        return getOutletcode();
    }

    public String getLoginId() {
        return super.getLoginid();
    }
    public  List<SalesDetails> getSalesDetails() {
        if(salesDetails==null){
            return new ArrayList<>();
        }
        return salesDetails;
    }

    public  List<SalesHistory> getSalesHistory() {
        if(salesHistory==null){
            return new ArrayList<>();
        }
        return salesHistory;
    }


    public boolean isOutletExists() {
        return outletExists;
    }

    public void setOutletExists(boolean outletExists) {
        this.outletExists = outletExists;
    }
}
