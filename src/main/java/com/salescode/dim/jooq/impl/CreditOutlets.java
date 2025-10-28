package com.salescode.dim.jooq.impl;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;


@Data
public class CreditOutlets extends com.salescode.dim.jooq.generated.tables.pojos.CreditOutlets implements Serializable {


    private String outletCode;
    private String outletName;
    private BigDecimal baseCreditLimit;
    private Integer creditDays;
    private Integer invoiceCount;
    @Min(value = 1, message = "Credit day code must be 1 or 2")
    @Max(value = 2, message = "Credit day code must be 1 or 2")
    private Integer creditDayCode; // 1: first open invoice date, 2: next month's 1st PJP date




    // Constructors
    public CreditOutlets() {
    }

    public CreditOutlets(String outletCode, String outletName, BigDecimal totalCredit, Integer creditDays, Integer maxInvoiceCount, String createdBy) {
        this.outletCode = outletCode;
        this.outletName = outletName;
        this.baseCreditLimit = totalCredit;
        this.creditDays = creditDays;
        this.invoiceCount = 0;
    }

}
