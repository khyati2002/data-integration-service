/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.dim.jooq.impl;

import com.applicate.services.channelkart.converters.LocationHierarchyDeserializer;
import com.applicate.services.channelkart.validations.ValidationResponseMessage;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;



public class ProductMetaData extends com.salescode.dim.jooq.generated.tables.pojos.Productmetadata implements Serializable {

    private static final long serialVersionUID = -8907648170103350925L;


    private String supplier;

    @Column(name = "skuCode")
    private String skuCode;

    /**
     * A unique code in case of duplicate SKUs with different MRP.
     * This field shall be used everywhere in place of SKU.
     */
    //@UniqueKey
    @NotBlank(message = ValidationResponseMessage.NOTBLANK)
    @Length(min = 0, max = 200, message = ValidationResponseMessage.LENGTH)
    private String batchCode;

    private String priceList;

    @Column(columnDefinition = "Decimal(15,8) default '0'")
    @Min(value = 0L, message = "The value must be positive")
    private Float packPtr;

    /**
     * packPtr double Transient field
     */
    @Transient
    private Double packPtrD;

    @Column(columnDefinition = "Decimal(15,8) default '0'")
    @Min(value = 0L, message = "The value must be positive")
    private Float casePtr;

    /**
     * casePtr double Transient field
     */
    @Transient
    private Double casePtrD;

    @Column(columnDefinition = "Decimal(15,5) default '0'")
    private float otherUnitPtr;

    /**
     * otherUnitPtr double Transient field
     */
    @Transient
    private Double otherUnitPtrD;

    @Column(columnDefinition = "Decimal(10,2) default '0'")
    private float gst;

    private Integer minQty;
    private Integer maxQty;
    private String level;

    @Column(columnDefinition = "Decimal(10,2) default '0'")
    private Float basePrice;
    private String tax;

    @Column(columnDefinition = "Decimal(10,2) default '0'")
    private float taxAmount;
    private String account;
    private String whCode;

    @JsonDeserialize(using = LocationHierarchyDeserializer.class)
    @Column(name = "location_hierarchy")
    private String locationHierarchy;

    //@UniqueKey
    private String channel;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date fromDate;


    /**
     * suggested selling price will be used to calculate retailer margin(margin value will be stored in extended attribute)
     */
    @Column(columnDefinition = "Decimal(15,5) default '0'")
    @Min(value = 0L, message = "The value must be positive")
    private Double ssp;

//    public Double getSsp() {
//        return ssp;
//    }

    public void setSsp(Double ssp) {
        this.ssp = ssp;
    }

//    public Date getFromDate() {
//        return fromDate;
//    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

//    public Date getToDate() {
//        return toDate;
//    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date toDate;

    @Column(columnDefinition = "Decimal(15,5) default '0'")
    @Min(value = 0L, message = "The value must be positive")
    private Float mrp;

    private String subChannel;

    @Column(name = "outletcode")
    private String outletCode;

    @Column(columnDefinition = "Decimal(15,5) default '0'")
    @Min(value = 0L, message = "The value must be positive")
    private Float caseMrp;

    @Column(columnDefinition = "Decimal(10,2) default '0'")
    private float otherUnitMrp;


    @Column(name = "batchId")
    @Length(min = 0, max = 200, message = ValidationResponseMessage.LENGTH)
    public String batchId;

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    /**
     * Priority of the catalogue
     */
    private int priority;

    /**
     * @return the priority
     */
//    public int getPriority() {
//        return priority;
//    }

    /**
     * @param priority set the priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * A product can be sold in cases or pieces. A case is group of pieces. If the
     * product is sold in cases, we need a mapping of many pieces make up the case
     */
    @Column(columnDefinition = "Decimal(10,2) default '0'")
    private Float caseToPieceQuantity;

//    public Float getCaseToPieceQuantity() {
//        return caseToPieceQuantity;
//    }

    public void setCaseToPieceQuantity(Float caseToPieceQuantity) {
        this.caseToPieceQuantity = caseToPieceQuantity;
    }

//    public float getCaseToOtherUnitQuantity() {
//        return caseToOtherUnitQuantity;
//    }

    public void setCaseToOtherUnitQuantity(float caseToOtherUnitQuantity) {
        this.caseToOtherUnitQuantity = caseToOtherUnitQuantity;
    }

//    public float getOtherUnitToPieceQuantity() {
//        return otherUnitToPieceQuantity;
//    }

    public void setOtherUnitToPieceQuantity(float otherUnitToPieceQuantity) {
        this.otherUnitToPieceQuantity = otherUnitToPieceQuantity;
    }

//    public float getPieceToOtherUnitQuantity() {
//        return pieceToOtherUnitQuantity;
//    }

    public void setPieceToOtherUnitQuantity(float pieceToOtherUnitQuantity) {
        this.pieceToOtherUnitQuantity = pieceToOtherUnitQuantity;
    }

    /**
     * A product can be sold in cases or other unit. A case is group of other unit.
     * If the product is sold in cases, we need a mapping of many other unit make up
     * the case
     */
    @Column(columnDefinition = "Decimal(10,2) default '0'")
    private float caseToOtherUnitQuantity;

    @Column(columnDefinition = "Decimal(10,2) default '0'")
    private float otherUnitToPieceQuantity;

    @Column(columnDefinition = "Decimal(10,2) default '0'")
    private float pieceToOtherUnitQuantity;

    private String productCode;

    @Column(columnDefinition = "Decimal(15,8) default '0'")
    @Min(value = 0L, message = "The value must be positive")
    private Float schemePrice;

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

//    public Float getCaseMrp() {
//        return caseMrp;
//    }

    public void setCaseMrp(Float caseMrp) {
        this.caseMrp = caseMrp;
    }

    public String getSubChannel() {
        return subChannel;
    }

    public void setSubChannel(String subChannel) {
        this.subChannel = subChannel;
    }

    /**
     * @return the priceList
     */
    public String getPriceList() {
        return priceList;
    }

    /**
     * @param priceList the priceList to set
     */
    public void setPriceList(String priceList) {
        this.priceList = priceList;
    }

    /**
     * @return the packPtr
     */
//    public Float getPackPtr() {
//        return packPtr;
//    }

    /**
     * @param packPtr the packPtr to set
     */
    public void setPackPtr(Float packPtr) {
        this.packPtr = packPtr;
    }

    /**
     * @return the casePtr
     */
//    public Float getCasePtr() {
//        return casePtr;
//    }

    /**
     * @param casePtr the casePtr to set
     */
    public void setCasePtr(Float casePtr) {
        this.casePtr = casePtr;
    }

    /**
     * @return the gst
     */
//    public float getGst() {
//        return gst;
//    }

    /**
     * @param gst the gst to set
     */
    public void setGst(float gst) {
        this.gst = gst;
    }

    /**
     * @return the minQty
     */
    public Integer getMinQty() {
        return minQty;
    }

    /**
     * @param minQty the minQty to set
     */
    public void setMinQty(Integer minQty) {
        this.minQty = minQty;
    }

    /**
     * @return the maxQty
     */
    public Integer getMaxQty() {
        return maxQty;
    }

    /**
     * @param maxQty the maxQty to set
     */
    public void setMaxQty(Integer maxQty) {
        this.maxQty = maxQty;
    }

    /**
     * @return the level
     */
    public String getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(String level) {
        this.level = level;
    }

    public String getSupplier() {
        return supplier;
    }

//    public void setSupplier(String supplier) {
//        this.supplier = supplier;
//    }

    /**
     * @return the basePrice
     */
//    public Float getBasePrice() {
//        return basePrice;
//    }

    /**
     * @param basePrice the basePrice to set
     */
    public void setBasePrice(Float basePrice) {
        this.basePrice = basePrice;
    }

    /**
     * @return the tax
     */
    public String getTax() {
        return tax;
    }

    /**
     * @param tax the tax to set
     */
    public void setTax(String tax) {
        this.tax = tax;
    }

    /**
     * @return the taxAmount
     */
//    public float getTaxAmount() {
//        return taxAmount;
//    }

    /**
     * @param taxAmount the taxAmount to set
     */
    public void setTaxAmount(float taxAmount) {
        this.taxAmount = taxAmount;
    }

    /**
     * @return the locationHierarchy
     */
    public String getLocationHierarchy() {
        return locationHierarchy;
    }

    /**
     * @param locationHierarchy the locationHierarchy to set
     */
    public void setLocationHierarchy(String locationHierarchy) {
        this.locationHierarchy = locationHierarchy;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    /**
     * @return the batchCode
     */
    public String getBatchCode() {
        return batchCode;
    }

    /**
     * @param batchCode the batchCode to set
     */
    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    /**
     * @return the otherUnitPtr
     */
//    public float getOtherUnitPtr() {
//        return otherUnitPtr;
//    }

    /**
     * @param otherUnitPtr the otherUnitPtr to set
     */
    public void setOtherUnitPtr(float otherUnitPtr) {
        this.otherUnitPtr = otherUnitPtr;
    }

    /**
     * @return the channel
     */
    public String getChannel() {
        return channel;
    }

    /**
     * @param channel the channel to set
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * @return the account
     */
    public String getAccount() {
        return account;
    }

    /**
     * @param account the account to set
     */
    public void setAccount(String account) {
        this.account = account;
    }

    /**
     * @return the whCode
     */
    public String getWhCode() {
        return whCode;
    }

    /**
     * @param whCode the whCode to set
     */
    public void setWhCode(String whCode) {
        this.whCode = whCode;
    }

    /**
     * @return the mrp
     */
//    public Float getMrp() {
//        return mrp;
//    }

    /**
     * @param mrp the mrp to set
     */
    public void setMrp(Float mrp) {
        this.mrp = mrp;
    }

    public String getOutletCode() {
        return outletCode;
    }

    public void setOutletCode(String outletCode) {
        this.outletCode = outletCode;
    }

//    @Override
//    public int hashCode() {
//        int result = ((batchCode == null) ? 0 : batchCode.hashCode());
//        result += ((supplier == null) ? 0 : supplier.hashCode());
//        return result;
//    }

    @Override
    public boolean equals(Object obj) {
        boolean returnFlag = true;
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }

        ProductMetaData other = (ProductMetaData) obj;

        if (!StringUtils.equals(batchCode, other.batchCode) || !StringUtils.equals(supplier, other.supplier) || !StringUtils.equals(locationHierarchy, other.locationHierarchy) || !StringUtils.equals(channel, other.channel)) {
            returnFlag = false;
        }
        return returnFlag;
    }

    @Override
    public boolean canHash() {
        return false;
    }

    @Override
    public String toString() {
        return "ProductMetaData [skuCode=" + skuCode + ", batchCode=" + batchCode
                + ", channel=" + channel + "]";
    }

    /**
     * @return the packPtrD
     */
    public Double getPackPtrD() {
        return packPtrD;
    }

    /**
     * @param packPtrD the packPtrD to set
     */
    public void setPackPtrD(Double packPtrD) {
        this.packPtrD = packPtrD;
    }

    /**
     * @return the casePtrD
     */
    public Double getCasePtrD() {
        return casePtrD;
    }

    /**
     * @param casePtrD the casePtrD to set
     */
    public void setCasePtrD(Double casePtrD) {
        this.casePtrD = casePtrD;
    }

    /**
     * @return the otherUnitPtrD
     */
    public Double getOtherUnitPtrD() {
        return otherUnitPtrD;
    }

    /**
     * @param otherUnitPtrD the otherUnitPtrD to set
     */
    public void setOtherUnitPtrD(Double otherUnitPtrD) {
        this.otherUnitPtrD = otherUnitPtrD;
    }

//    public Float getOtherUnitMrp() {
//        return otherUnitMrp;
//    }

    public void setOtherUnitMrp(Float otherUnitMrp) {
        this.otherUnitMrp = otherUnitMrp;
    }

//    public Float getSchemePrice() {
//        return schemePrice;
//    }

    public void setSchemePrice(Float schemePrice) {
        this.schemePrice = schemePrice;
    }
}