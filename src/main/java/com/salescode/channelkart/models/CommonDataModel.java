package com.salescode.channelkart.models;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.channelkart.models.diff.Change;
import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.utils.CdmDiffUtil;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.*;

public abstract class CommonDataModel implements Serializable {

    @Transient
    private boolean isCreate;

    @Getter
    @Setter
    @Transient
    @JsonIgnore
    private List<String> preProcessPipelineException;

    @Transient
    @JsonInclude()
    private Set<Change<Serializable>> changes;

    @Getter
    private transient CommonDataModel oldModel;

    public CommonDataModel() {
        this.setId(UUID.randomUUID().toString());
        this.setVersion(0);
    }

    public abstract String getId();

    public abstract void setId(String id);

    public abstract Integer getVersion();

    public abstract void setVersion(Integer version);

    public abstract ActiveStatus getActiveStatus();

    public abstract void setActiveStatus(ActiveStatus activeStatus);

    public abstract String getActiveStatusReason();

    public abstract void setActiveStatusReason(String activeStatusReason);

    public abstract Date getCreationTime();

    public abstract void setCreationTime(Date creationTime);

    public abstract Date getLastModifiedTime();

    public abstract void setLastModifiedTime(Date lastModifiedTime);

    public abstract String getCreatedBy();

    public abstract void setCreatedBy(String createdBy);

    public abstract String getModifiedBy();

    public abstract void setModifiedBy(String modifiedBy);

    public abstract String getLob();

    public abstract void setLob(String lob);

    public abstract String getSource();

    public abstract void setSource(String source);

    public abstract JsonNode getExtendedAttributes();

    public abstract void setExtendedAttributes(JsonNode extendedAttributes);

    public boolean isCreate() {
        return isCreate;
    }

    public void setCreate(boolean create) {
        isCreate = create;
    }

    @JsonGetter
    public Set<Change<Serializable>> getChanges() {
        if (this.changes == null) {
            this.changes = findChanges();
        }
        return this.changes;
    }

    @JsonSetter
    public void setChanges(Set<Change<Serializable>> changes) {
        this.changes = changes;
    }

    public Set<Change<Serializable>> findChanges() {
        return oldModel == null ? Collections.emptySet() : CdmDiffUtil.getChanges(this, this.getOldModel());
    }

    public abstract String getHash();

    public abstract void setHash(String hash);


    public void addPreProcessPipelineException(String stackTrace) {
        if (preProcessPipelineException == null) {
            this.preProcessPipelineException = new ArrayList<>();
        }
        preProcessPipelineException.add(stackTrace);
    }

    @JsonIgnore
    @Transient
    public boolean canHash() {
        return false;
    }

    public void setOldModel(CommonDataModel oldModel) {
        this.oldModel = oldModel;
        setChanges(null);
    }


}