package com.salescode.channelkart.models;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.channelkart.converters.ActiveStatus;
import com.salescode.channelkart.models.diff.Change;
import com.salescode.channelkart.utils.CdmDiffUtil;
import com.salescode.channelkart.utils.ReflectionUtils;
import com.salescode.jooq.generated.tables.pojos.CkOutletDetails;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

public abstract class CommonDataModel implements Serializable {


    public static final Set<String> EXCLUDED_PROPERTIES =
            Set.of("hash", "forceHash", "isCreate", "id", "createdBy", "creationTime",
                    "oldModel", "modifiedBy", "changes", "changed", "lastModifiedTime",
                    "version", "lob","$jacocoData");

    public static final Set<String> CAN_HASH =
            Set.of("CkOutletDetails","CkUser");

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

//   public abstract String extractUniqueKey();
//   public abstract CommonDataModelService<?> getService();

    public void addPreProcessPipelineException(String stackTrace) {
        if (preProcessPipelineException == null) {
            this.preProcessPipelineException = new ArrayList<>();
        }
        preProcessPipelineException.add(stackTrace);
    }

    @JsonIgnore
    @Transient
    public boolean canHash() {
        String className = this.getClass().getSimpleName();
        return CAN_HASH.contains(className);
    }

    public void setOldModel(CommonDataModel oldModel) {
        this.oldModel = oldModel;
        setChanges(null);
    }

    @JsonIgnore
    public String hash() {
        return hash(new HashSet<>(), 0);
    }

    public boolean forceHash(){
        return false;
    }

    @JsonIgnore
    private String hash(Set<CommonDataModel> visitedModels, int level) {
        int currentLevel = level + 1;
        if (visitedModels.contains(this)) {
            return "";
        }
        visitedModels.add(this);
        List<Object> props = ReflectionUtils.extractInstanceValues(this, EXCLUDED_PROPERTIES);
        Object[] objectsToHash = props.stream()
                .filter(Objects::nonNull)
                .flatMap(item -> toItems(item, currentLevel))
                .map(value -> toHashableItem(value, visitedModels, currentLevel))
                .filter(Objects::nonNull)
                .toArray();
        return String.valueOf(Objects.hash(objectsToHash));
    }

    private Stream<?> toItems(Object item, int level) {
        if (item instanceof Collection) {
            if (level > 1) {
                return Stream.of("");
            }
            return ((Collection<?>) item).stream();
        }
        return Stream.of(item);
    }

    private Object toHashableItem(Object value, Set<CommonDataModel> visitedModels, int level) {
        if (level > 1 && value instanceof CommonDataModel) {
            // more than one level of caching is not required
            return "";
        }
        if (value instanceof CommonDataModel && (visitedModels.contains(value))) {
            // hash is already calculated or still calculating ... no need of check
            return "";
        }
        if (value instanceof CommonDataModel) {
            return ((CommonDataModel) value).hash(visitedModels, level);
        }
        if(JsonNode.class.isAssignableFrom(value.getClass()) || Enum.class.isAssignableFrom(value.getClass()))
            return value.toString();
        return value;
    }

}