package com.salescode.dim.jooq.impl;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a credit outlet.
 * Extends BaseEntity for common audit fields and functionality.
 */
@Getter
@Setter
public class CreditOutlets extends CommonDataModel {


    private String id;


    @NotBlank(message = "Outlet code is required")
    @Size(max = 50, message = "Outlet code cannot exceed 50 characters")
    private String outletCode;

    @Size(max = 200, message = "Outlet name cannot exceed 200 characters")
    private String outletName;


    @DecimalMin(value = "0.0", message = "Base credit limit cannot be negative")
    private BigDecimal baseCreditLimit;

    @DecimalMin(value = "0.0", message = "Current credit limit cannot be negative")
    private BigDecimal currentCreditLimit;


    @Min(value = 0, message = "Credit days cannot be negative")
    private Integer creditDays;


    @Min(value = 0, message = "Invoice count cannot be negative")
    private Integer invoiceCount;


    @Min(value = 1, message = "Credit day code must be 1 or 2")
    @Max(value = 2, message = "Credit day code must be 1 or 2")
    private Integer creditDayCode; // 1: first open invoice date, 2: next month's 1st PJP date

    private BigDecimal availableCredit;


    private String lob;

    // Constructors
    public CreditOutlets() {
        super();
    }



    public String getOutletCode() {
        return outletCode;
    }

    public void setOutletCode(String outletCode) {
        this.outletCode = outletCode;
    }

    public String getOutletName() {
        return outletName;
    }

    public void setOutletName(String outletName) {
        this.outletName = outletName;
    }



    public BigDecimal getBaseCreditLimit() {
        return baseCreditLimit;
    }

    public void setBaseCreditLimit(BigDecimal baseCreditLimit) {
        this.baseCreditLimit = baseCreditLimit;
    }

    public BigDecimal getCurrentCreditLimit() {
        return currentCreditLimit;
    }

    public void setCurrentCreditLimit(BigDecimal currentCreditLimit) {
        this.currentCreditLimit = currentCreditLimit;
    }



    public Integer getCreditDays() {
        return creditDays;
    }

    public void setCreditDays(Integer creditDays) {
        this.creditDays = creditDays;
    }


    public Integer getInvoiceCount() {
        return invoiceCount;
    }

    public void setInvoiceCount(Integer invoiceCount) {
        this.invoiceCount = invoiceCount;
    }



    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id=id;
    }

    @Override
    public Integer getVersion() {
        return 0;
    }

    @Override
    public void setVersion(Integer version) {

    }

    @Override
    public ActiveStatus getActiveStatus() {
        return null;
    }

    @Override
    public void setActiveStatus(ActiveStatus activeStatus) {

    }

    @Override
    public String getActiveStatusReason() {
        return "";
    }

    @Override
    public void setActiveStatusReason(String activeStatusReason) {

    }

    @Override
    public LocalDateTime getCreationTime() {
        return null;
    }

    @Override
    public void setCreationTime(LocalDateTime creationTime) {

    }

    @Override
    public LocalDateTime getLastModifiedTime() {
        return null;
    }

    @Override
    public void setLastModifiedTime(LocalDateTime lastModifiedTime) {

    }

    @Override
    public String getCreatedBy() {
        return "";
    }

    @Override
    public void setCreatedBy(String createdBy) {

    }

    @Override
    public String getModifiedBy() {
        return "";
    }

    @Override
    public void setModifiedBy(String modifiedBy) {

    }

    public String getLob() {
        return lob;
    }

    public void setLob(String lob) {
        this.lob = lob;
    }

    @Override
    public String getSource() {
        return "";
    }

    @Override
    public void setSource(String source) {

    }

    @Override
    public JsonNode getExtendedAttributes() {
        return null;
    }

    @Override
    public void setExtendedAttributes(JsonNode extendedAttributes) {

    }

    @Override
    public String getHash() {
        return "";
    }

    @Override
    public void setHash(String hash) {

    }

    public Integer getCreditDayCode() {
        return creditDayCode;
    }

    public void setCreditDayCode(Integer creditDayCode) {
        this.creditDayCode = creditDayCode;
    }


    @Override
    public int hashCode() {
        // Use BaseEntity's ID-based hash if ID exists, otherwise use business key
        return getId() != null ? super.hashCode() : Objects.hash(outletCode);
    }

}
