package com.salescode.dim.jooq.impl;

import com.applicate.services.channelkart.converters.ClientTimeZoneStringToUTCDateConverter;
import com.applicate.services.channelkart.converters.DateToClientTimeZoneStringConverter;
import com.applicate.services.channelkart.converters.JSONObjectConverter;
import com.applicate.services.channelkart.converters.SavedLocationHierarchyDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;


import javax.persistence.*;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;


@Getter
@Setter
public class SecondaryProduct extends com.salescode.dim.jooq.generated.tables.pojos.SecondaryProduct implements Serializable {
    private static final long serialVersionUID = 1L;

    /* we can store competitor name for other features like competitor focus product, SOS of competitors*/
    private String companyName;

    /* This specifies for which feature it is stored like focus brand ,competitor sku info ..etc*/
    private String feature;

    @Column(columnDefinition = "json")
    @Convert(converter = JSONObjectConverter.class)
    private JsonNode blobKeys;

    @Column(name = "skuCode")
    private String skuCode;
    private String batchCode;
    private String productCode;
    private String product;
    private String skuDescription;
    private String description;
    private String marketSkuCode;
    private String marketSku;
    private String category;
    private String categoryCode;
    private String subCategory;
    private String subCategoryCode;
    private String brand;
    private String brandCode;
    private String itemType;
    private String itemName;
    private String productTag;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(converter = ClientTimeZoneStringToUTCDateConverter.class)
    @JsonSerialize(converter = DateToClientTimeZoneStringConverter.class)
    private Date startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(converter = ClientTimeZoneStringToUTCDateConverter.class)
    @JsonSerialize(converter = DateToClientTimeZoneStringConverter.class)
    private Date endTime;

    /* we can store feature specific information like minbaseqty value for bil */
    private double quantity;

    @Column(name = "outletcode")
    private String outletCode;

    private String outletCategory;
    private String outletClass;
    private String account;
    private String channel;
    private String outletType;
    private String beat;
    private String filterKey;
    private String filterValue;
    private String loginid;

    @JsonDeserialize(using = SavedLocationHierarchyDeserializer.class)
    @Column(name = "location_hierarchy")
    private String locationHierarchy;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

//    public String getFeature() {
//        return feature;
//    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

//    public JsonNode getBlobKeys() {
//        return blobKeys;
//    }

    public void setBlobKeys(JsonNode blobKeys) {
        this.blobKeys = blobKeys;
    }

//    public String getSkuCode() {
//        return skuCode;
//    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getSkuDescription() {
        return skuDescription;
    }

    public void setSkuDescription(String skuDescription) {
        this.skuDescription = skuDescription;
    }

    public String getMarketSkuCode() {
        return marketSkuCode;
    }

    public void setMarketSkuCode(String marketSkuCode) {
        this.marketSkuCode = marketSkuCode;
    }

    public String getMarketSku() {
        return marketSku;
    }

    public void setMarketSku(String marketSku) {
        this.marketSku = marketSku;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getSubCategoryCode() {
        return subCategoryCode;
    }

    public void setSubCategoryCode(String subCategoryCode) {
        this.subCategoryCode = subCategoryCode;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getBrandCode() {
        return brandCode;
    }

    public void setBrandCode(String brandCode) {
        this.brandCode = brandCode;
    }

//    public Date getStartTime() {
//        return startTime;
//    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

//    public Date getEndTime() {
//        return endTime;
//    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

//    public double getQuantity() {
//        return quantity;
//    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getOutletCode() {
        return outletCode;
    }

    public void setOutletCode(String outletCode) {
        this.outletCode = outletCode;
    }

    public String getOutletCategory() {
        return outletCategory;
    }

    public void setOutletCategory(String outletCategory) {
        this.outletCategory = outletCategory;
    }

    public String getOutletClass() {
        return outletClass;
    }

    public void setOutletClass(String outletClass) {
        this.outletClass = outletClass;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getOutletType() {
        return outletType;
    }

    public void setOutletType(String outletType) {
        this.outletType = outletType;
    }

//    public String getBeat() {
//        return beat;
//    }

    public void setBeat(String beat) {
        this.beat = beat;
    }

    public String getLocationHierarchy() {
        return locationHierarchy;
    }

    public void setLocationHierarchy(String locationHierarchy) {
        this.locationHierarchy = locationHierarchy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getFilterKey() {
        return filterKey;
    }

    public void setFilterKey(String filterKey) {
        this.filterKey = filterKey;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public void setFilterValue(String filterValue) {
        this.filterValue = filterValue;
    }

    public String getLoginid() {
        return loginid;
    }

    public void setLoginid(String loginid) {
        this.loginid = loginid;
    }

    public String getProductTag() { return productTag; }

    public void setProductTag(String productTag) { this.productTag = productTag; }
}
