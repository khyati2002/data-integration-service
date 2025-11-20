package com.salescode.dim.jooq.impl;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.jooq.generated.enums.DmsVanLoadoutActiveStatus;
import com.salescode.dim.jooq.generated.tables.pojos.DmsVanLoadout;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Getter
@Setter
@AllArgsConstructor
public class VanLoadout extends CommonDataModel {

    private DmsVanLoadout dmsVanLoadout;

    private List<VanItems> vanItemsList;

    public VanLoadout() {
        this.dmsVanLoadout = new DmsVanLoadout();
    }

    @Override
    public String getId() {
        return dmsVanLoadout != null ? dmsVanLoadout.getId() : null;
    }

    @Override
    public void setId(String id) {
        if (dmsVanLoadout == null) {
            dmsVanLoadout = new DmsVanLoadout();
        }
        dmsVanLoadout.setId(id);
    }

    @Override
    public Integer getVersion() {
        return dmsVanLoadout != null ? dmsVanLoadout.getVersion() : null;
    }

    @Override
    public void setVersion(Integer version) {
        if (dmsVanLoadout == null) {
            dmsVanLoadout = new DmsVanLoadout();
        }
        dmsVanLoadout.setVersion(version);
    }

    @Override
    public ActiveStatus getActiveStatus() {
        return dmsVanLoadout != null ? ActiveStatus.valueOf(dmsVanLoadout.getActiveStatus().getLiteral().toLowerCase(Locale.ROOT)) : null;
    }

    @Override
    public void setActiveStatus(ActiveStatus activeStatus) {
        if (dmsVanLoadout == null) {
            dmsVanLoadout = new DmsVanLoadout();
        }
        dmsVanLoadout.setActiveStatus(DmsVanLoadoutActiveStatus.valueOf(activeStatus.getStatus().toUpperCase(Locale.ROOT)));
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
        return dmsVanLoadout != null ? dmsVanLoadout.getCreationTime() : null;
    }

    @Override
    public void setCreationTime(LocalDateTime creationTime) {
        if (dmsVanLoadout == null) {
            dmsVanLoadout = new DmsVanLoadout();
        }
        dmsVanLoadout.setCreationTime(creationTime);
    }

    @Override
    public LocalDateTime getLastModifiedTime() {
        return dmsVanLoadout != null ? dmsVanLoadout.getLastModifiedTime() : null;
    }

    @Override
    public void setLastModifiedTime(LocalDateTime lastModifiedTime) {
        if (dmsVanLoadout == null) {
            dmsVanLoadout = new DmsVanLoadout();
        }
        dmsVanLoadout.setLastModifiedTime(lastModifiedTime);
    }

    @Override
    public String getCreatedBy() {
        return dmsVanLoadout != null ? dmsVanLoadout.getCreatedBy() : null;
    }

    @Override
    public void setCreatedBy(String createdBy) {
        if (dmsVanLoadout == null) {
            dmsVanLoadout = new DmsVanLoadout();
        }
        dmsVanLoadout.setCreatedBy(createdBy);
    }

    @Override
    public String getModifiedBy() {
        return dmsVanLoadout != null ? dmsVanLoadout.getModifiedBy() : null;
    }

    @Override
    public void setModifiedBy(String modifiedBy) {
        if (dmsVanLoadout == null) {
            dmsVanLoadout = new DmsVanLoadout();
        }
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
