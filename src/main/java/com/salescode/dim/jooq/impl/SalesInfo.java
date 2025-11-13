package com.salescode.dim.jooq.impl;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SalesInfo implements Serializable {
    private BigDecimal gstAmount;
    private BigDecimal tcsAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String invoiceStatus;
}
