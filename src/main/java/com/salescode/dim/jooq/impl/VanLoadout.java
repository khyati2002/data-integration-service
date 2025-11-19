package com.salescode.dim.jooq.impl;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.jooq.generated.tables.pojos.DmsVanLoadout;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class VanLoadout extends CommonDataModel {

    private DmsVanLoadout dmsVanLoadout;

    private List<VanItems> vanItemsList;


    @Override
    public String getId() {
        return dmsVanLoadout.getId();
    }

    @Override
    public void setId(String id) {
        dmsVanLoadout.setId(id);
    }

    @Override
    public Integer getVersion() {
        return dmsVanLoadout.getVersion();
    }

    @Override
    public void setVersion(Integer version) {
        dmsVanLoadout.setVersion(version);
    }

    @Override
    public ActiveStatus getActiveStatus() {
        return dmsVanLoadout.getActiveStatus();
    }

    @Override
    public void setActiveStatus(ActiveStatus activeStatus) {
        dmsVanLoadout.setActiveStatus(activeStatus);
    }

    @Override
    public String getActiveStatusReason() {
        return "";
    }

    @Override
    public void setActiveStatusReason(String activeStatusReason) {
        // column does not exist
    }

    @Override
    public LocalDateTime getCreationTime() {
        return dmsVanLoadout.getCreationTime();
    }

    @Override
    public void setCreationTime(LocalDateTime creationTime) {
        dmsVanLoadout.setCreationTime(creationTime);
    }

    @Override
    public LocalDateTime getLastModifiedTime() {
        return dmsVanLoadout.getLastModifiedTime();
    }

    @Override
    public void setLastModifiedTime(LocalDateTime lastModifiedTime) {
        dmsVanLoadout.setLastModifiedTime(lastModifiedTime);
    }

    @Override
    public String getCreatedBy() {
        return dmsVanLoadout.getCreatedBy();
    }

    @Override
    public void setCreatedBy(String createdBy) {
        dmsVanLoadout.setCreatedBy(createdBy);
    }

    @Override
    public String getModifiedBy() {
        return dmsVanLoadout.getModifiedBy();
    }

    @Override
    public void setModifiedBy(String modifiedBy) {
        dmsVanLoadout.setModifiedBy(modifiedBy);
    }

    @Override
    public String getLob() {
        return "";
    }

    @Override
    public void setLob(String lob) {
        // column does not exist
    }

    @Override
    public String getSource() {
        return "";
    }

    @Override
    public void setSource(String source) {
        // column does not exist
    }

    @Override
    public JsonNode getExtendedAttributes() {
        return null;
    }

    @Override
    public void setExtendedAttributes(JsonNode extendedAttributes) {
        // column does not exist
    }

    @Override
    public String getHash() {
        return "";
    }

    @Override
    public void setHash(String hash) {
        // column does not exist
    }
}
