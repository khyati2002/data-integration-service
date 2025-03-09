package com.applicate.services.channelkart.models;

import com.applicate.services.channelkart.models.diff.Change;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.salescode.dim.utils.ReflectionUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

/**
 * Abstract base model with common properties and change tracking.
 */
@Getter
@Setter
@Accessors(chain = true)
public abstract class CommonDataModel implements Serializable {

    public static final Set<String> EXCLUDED_PROPERTIES = Set.of("hash", "forceHash", "isCreate", "id", "createdBy", "creationTime", "oldModel", "modifiedBy", "changes", "changed", "lastModifiedTime", "version", "lob", "$jacocoData");
    private static final long serialVersionUID = 1L;

    private boolean changed = true;


    transient private boolean isCreate;

    @JsonIgnore
    private boolean forceHash;

    @Getter
    @Setter

    transient @JsonIgnore
    private List<String> preProcessPipelineException;


    transient @JsonInclude()
    private Set<Change<Serializable>> changes;

    @Getter
    private transient CommonDataModel oldModel;

    /**
     * Default constructor initializing version to 0.
     */
    public CommonDataModel() {
        // Using the abstract setter; ensure concrete implementations handle nulls appropriately.
        this.setVersion(Integer.valueOf(0));
    }

    public abstract String getId();

    public abstract void setId(String id);

    public abstract Integer getVersion();

    public abstract void setVersion(Integer version);

    public abstract ActiveStatus getActiveStatus();

    public abstract void setActiveStatus(ActiveStatus activeStatus);

    public abstract String getActiveStatusReason();

    public abstract void setActiveStatusReason(String activeStatusReason);

    public abstract LocalDateTime getCreationTime();

    public abstract void setCreationTime(LocalDateTime creationTime);

    public abstract LocalDateTime getLastModifiedTime();

    public abstract void setLastModifiedTime(LocalDateTime lastModifiedTime);

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

    public abstract String getHash();

    public abstract void setHash(String hash);

    @JsonIgnore
    public String hash() {
        return hash(new HashSet<>(), 0);
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

    private Object toHashableItem(Object value, Set<CommonDataModel> visitedModels, int level) {
        if (level > 1 && value instanceof CommonDataModel) {
            // More than one level of caching is not required.
            return "";
        }
        if (value instanceof CommonDataModel && visitedModels.contains(value)) {
            return "";
        }
        if (value instanceof CommonDataModel) {
            return ((CommonDataModel) value).hash(visitedModels, level);
        }
        if (value instanceof JsonNode || value instanceof Enum) {
            return value.toString();
        }
        return value;
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

    public void addPreProcessPipelineException(String stackTrace) {
        if (preProcessPipelineException == null) {
            this.preProcessPipelineException = new ArrayList<>();
        }
        preProcessPipelineException.add(stackTrace);
    }

    @JsonIgnore
    public boolean canHash() {
        return false;
    }

    public void setOldModel(CommonDataModel oldModel) {
        this.oldModel = oldModel;
        setChanges(null);
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

//    @JsonIgnore
//    public CommonDataModelService<?> getService() {
//        return ServiceLocator.lookup(this.getClass());
//    }

//    @JsonIgnore
//    protected String extractUniqueKey() {
//        CommonDataModelService<?> service = ServiceLocator.lookup(this.getClass());
//        if (service != null) {
//            return service.getKey(this);
//        }
//        return StringUtils.isNotEmpty(id) ? id : "";
//    }

}