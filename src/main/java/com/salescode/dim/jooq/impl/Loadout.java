package com.salescode.dim.jooq.impl;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.jooq.generated.tables.pojos.DmsLoadout;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Loadout extends CommonDataModel {


    private DmsLoadout dmsLoadout;

    private List<LoadoutDetails> loadoutDetailsList;

    public Loadout() {
        this.dmsLoadout = new DmsLoadout();
    }

    @Override
    public String getId() {
        return dmsLoadout != null ? dmsLoadout.getId() : null;
    }

    @Override
    public void setId(String id) {
        if (dmsLoadout == null) {
            dmsLoadout = new DmsLoadout();
        }
        dmsLoadout.setId(id);
    }

    @Override
    public Integer getVersion() {
        return dmsLoadout != null ? dmsLoadout.getVersion() : null;
    }

    @Override
    public void setVersion(Integer version) {
        if (dmsLoadout == null) {
            dmsLoadout = new DmsLoadout();
        }
        dmsLoadout.setVersion(version);
    }

    @Override
    public ActiveStatus getActiveStatus() {
        return dmsLoadout != null ? dmsLoadout.getActiveStatus() : null;
    }

    @Override
    public void setActiveStatus(ActiveStatus activeStatus) {
        if (dmsLoadout == null) {
            dmsLoadout = new DmsLoadout();
        }
        dmsLoadout.setActiveStatus(activeStatus);
    }

    @Override
    public String getActiveStatusReason() {
        return "";
    }

    @Override
    public void setActiveStatusReason(String activeStatusReason) {
        // not required
    }

    @Override
    public LocalDateTime getCreationTime() {
        return dmsLoadout != null ? dmsLoadout.getCreationTime() : null;
    }

    @Override
    public void setCreationTime(LocalDateTime creationTime) {
        if (dmsLoadout == null) {
            dmsLoadout = new DmsLoadout();
        }
        dmsLoadout.setCreationTime(creationTime);
    }

    @Override
    public LocalDateTime getLastModifiedTime() {
        return dmsLoadout != null ? dmsLoadout.getLastModifiedTime() : null;
    }

    @Override
    public void setLastModifiedTime(LocalDateTime lastModifiedTime) {
        if (dmsLoadout == null) {
            dmsLoadout = new DmsLoadout();
        }
        dmsLoadout.setLastModifiedTime(lastModifiedTime);
    }

    @Override
    public String getCreatedBy() {
        return dmsLoadout != null ? dmsLoadout.getCreatedBy() : null;
    }

    @Override
    public void setCreatedBy(String createdBy) {
        if (dmsLoadout == null) {
            dmsLoadout = new DmsLoadout();
        }
        dmsLoadout.setCreatedBy(createdBy);
    }

    @Override
    public String getModifiedBy() {
        return dmsLoadout != null ? dmsLoadout.getModifiedBy() : null;
    }

    @Override
    public void setModifiedBy(String modifiedBy) {
        if (dmsLoadout == null) {
            dmsLoadout = new DmsLoadout();
        }
        dmsLoadout.setModifiedBy(modifiedBy);
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
