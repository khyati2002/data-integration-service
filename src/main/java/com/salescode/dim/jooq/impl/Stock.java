package com.salescode.dim.jooq.impl;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true, value = {"id", "changed", "create", "changes", "oldModel", "reqId","operationPerformed", "" ,"version", "activeStatus", "activeStatusReason", "creationTime", "lastModifiedTime", "createdBy", "modifiedBy", "lob", "source", "extendedAttributes", "hash"})
@NoArgsConstructor
@AllArgsConstructor
public class Stock extends CommonDataModel {


    private String supplier;
    private String warehouseId;
    private String skuCode;
    private Integer caseQty;
    private Integer pieceQty;
    private Integer otherQty;
    private String batchId;

    public Stock(String skuCode, Integer caseQty, Integer pieceQty, Integer otherQty, String batchId) {
        super();
        this.skuCode = skuCode;
        this.caseQty = caseQty;
        this.pieceQty = pieceQty;
        this.otherQty = otherQty;
        this.batchId = batchId;
    }


    @Override
    public String getId() {
        return "";
    }

    @Override
    public void setId(String id) {

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

    @Override
    public String getLob() {
        return "";
    }

    @Override
    public void setLob(String lob) {

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
}
