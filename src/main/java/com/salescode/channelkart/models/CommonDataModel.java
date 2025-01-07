package com.salescode.channelkart.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salescode.channelkart.converters.ActiveStatusConverter;
import com.salescode.channelkart.converters.DateToClientTimeZoneStringConverter;
import com.salescode.channelkart.converters.JSONObjectConverter;
import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.models.diff.Change;
import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.utils.CdmDiffUtil;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.ReflectionUtils;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

@Getter
@Setter
@MappedSuperclass
public class CommonDataModel implements Serializable {

    public static final Set<String> EXCLUDED_PROPERTIES = Set.of("hash", "forceHash", "isCreate", "id", "createdBy", "creationTime", "oldModel", "modifiedBy", "changes", "changed", "lastModifiedTime", "version", "lob","$jacocoData");

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


    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "com.salescode.channelkart.services.UUIDIdentifier")
    private String id;

    @Version
    private Integer version;

    @Convert(converter = ActiveStatusConverter.class)
    private ActiveStatus activeStatus;

    private String activeStatusReason;

    @JsonSerialize(converter = DateToClientTimeZoneStringConverter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date creationTime;


    @JsonSerialize(converter = DateToClientTimeZoneStringConverter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date lastModifiedTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String createdBy;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String modifiedBy;

    private String lob;

    private String source;

    @Column(columnDefinition = "json")
    @Convert(converter = JSONObjectConverter.class)
    private JsonNode extendedAttributes;

    @Column(unique = true, columnDefinition = "LONGTEXT")
    @JsonIgnore
    private String hash;

    @JsonIgnore
    @Transient
    public boolean canHash() {
        return false;
    }


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


    public void addPreProcessPipelineException(String stackTrace) {
        if (preProcessPipelineException == null) {
            this.preProcessPipelineException = new ArrayList<>();
        }
        preProcessPipelineException.add(stackTrace);
    }


    public void setOldModel(CommonDataModel oldModel) {
        this.oldModel = oldModel;
        setChanges(null);
    }


    @JsonIgnore
    @Transient
    public boolean isActive() {
        return activeStatus != null && activeStatus.equals(ActiveStatus.ACTIVE);
    }

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

    public boolean forceHash(){
        return false;
    }
    @JsonIgnore
    public <M extends CommonDataModel> boolean compare(final M obj2, boolean ignoreId) throws CustomRuntimeException {
        try {
            Assert.notNull(obj2, "Illegal paramter value for comparision");
            if(this.getClass() != obj2.getClass()) {
                throw new IllegalArgumentException("Comparing objects must belong to same class. Argument : "+obj2.getClass().getName());
            }
        }catch(IllegalArgumentException ex) {
            throw new CustomRuntimeException(ex);
        }
        Set<String> uniqueKeys= EntityUtils.get().getUniqueKeys(obj2.getClass());
        uniqueKeys.add(id);
        if(ignoreId) {
            uniqueKeys.remove(id);
        }
        return uniqueKeys.stream().allMatch(p -> Objects.equals(com.salescode.channelkart.utils.ReflectionUtils.readData(this, p),
                com.salescode.channelkart.utils.ReflectionUtils.readData(obj2, p)));
    }


}