package com.salescode.dim.jooq.impl;


import com.salescode.dim.jooq.generated.tables.pojos.GrnInfo;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.beans.Transient;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GRNInfo extends com.salescode.dim.jooq.generated.tables.pojos.GrnInfo implements Serializable {


    private List<SalesDetails> salesDetails;
    private List<SalesHistory> salesHistory;
    private String invoiceNumber;
    private boolean update;

    public GRNInfo() {
        super();
    }

    private GRNInfo(GrnInfo grnInfo) {
        super(grnInfo);
    }

    public static GRNInfo of(GRNInfo grnInfo) {
        if (grnInfo == null) {
            return null;
        }
        return new GRNInfo(grnInfo);
    }

}
