package com.salescode.dis.insights.orders.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderSummaryResponse {

    private String lob;
    private Integer retentionHours;
    private Long totalRecords;
    private Long readSuccess;
    private Long readFailure;
    private Long readPending;
    private Long processSuccess;
    private Long processFailure;
    private Long processPending;
    private Long saveSuccess;
    private Long saveFailure;
    private Long savePending;
    private Long publishSuccess;
    private Long publishNA;

    // Constructors
    public OrderSummaryResponse() {}

    public OrderSummaryResponse(String lob, Integer retentionHours, Long totalRecords,
                                Long readSuccess, Long readFailure, Long readPending,
                                Long processSuccess, Long processFailure, Long processPending,
                                Long saveSuccess, Long saveFailure, Long savePending, Long publishSuccess, Long publishNA) {
        this.lob = lob;
        this.retentionHours = retentionHours;
        this.totalRecords = totalRecords;
        this.readSuccess = readSuccess;
        this.readFailure = readFailure;
        this.readPending = readPending;
        this.processSuccess = processSuccess;
        this.processFailure = processFailure;
        this.processPending = processPending;
        this.saveSuccess = saveSuccess;
        this.saveFailure=saveFailure;
        this.savePending=savePending;
        this.publishSuccess=publishSuccess;
        this.publishNA=publishNA;
    }
    }
