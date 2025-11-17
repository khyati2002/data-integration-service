package com.salescode.dim.jooq.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesInfo implements Serializable {
    private BigDecimal gstAmount;
    private BigDecimal tcsAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String invoiceStatus;
}
