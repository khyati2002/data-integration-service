package com.applicate.services.channelkart.models;

import com.applicate.services.channelkart.converters.ActiveStatusConverter;
import com.applicate.services.channelkart.converters.DateToClientTimeZoneStringConverter;
import com.applicate.services.channelkart.converters.JSONObjectConverter;
import com.applicate.services.channelkart.exceptions.CustomRuntimeException;
import com.applicate.services.channelkart.masking.MaskField;
import com.applicate.services.channelkart.models.diff.Change;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.CommonDataModelService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

@MappedSuperclass
public class CommonDataModel implements Serializable {

    public static final Set<String> EXCLUDED_PROPERTIES =
            Set.of("hash", "forceHash", "isCreate", "id", "createdBy", "creationTime",
                    "oldModel", "modifiedBy", "changes", "changed", "lastModifiedTime",
                    "version", "lob","$jacocoData");

    /**
     * This is a Super Class of all Entity classes. All the Entities extend this class and the attributes are common for
     * the sub classes. These attributes are auto populated when the entity is saved.
     *
     * @see com.applicate.services.channelkart.services.AbstractCDMService#fillCommonAttributes(CommonDataModel) (T)
     */
    private static final long serialVersionUID = 1L;

    /**
     * The entity(user) who created the record
     */
    @JsonProperty(access = Access.READ_ONLY)
    @MaskField
    private String createdBy;

    @Transient
    private transient CommonDataModel oldModel;

    public boolean isForceHash() {
        return forceHash;
    }

    public void setForceHash(boolean forceHash) {
        this.forceHash = forceHash;
    }

    @Transient
    @JsonIgnore
    private boolean forceHash;

    /**
     * The entity(user) who modified the record
     */
    @MaskField
    @JsonProperty(access = Access.READ_ONLY)
    private String modifiedBy;

    /**
     * Time of creation
     */
    @JsonSerialize(converter = DateToClientTimeZoneStringConverter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty(access = Access.READ_ONLY)
    private Date creationTime;

    /**
     * Time last modified
     */
    @JsonSerialize(converter = DateToClientTimeZoneStringConverter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty(access = Access.READ_ONLY)
    private Date lastModifiedTime;

    /**
     * Line of business - It means that, each Company or Client will have a unique identifier.
     * The same companies can have multiple businesses. In such cases, there will be a different lob for each business.
     * lob will always be unique per business
     */
    private String lob;

    /**
     * Unique ID of the entity or record. It is unique across DB.
     */
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "com.applicate.services.channelkart.services.UUIDIdentifier")
    private String id;

    /**
     * Indicates if the entity or record is active or not
     */
    @Convert(converter = ActiveStatusConverter.class)
    private ActiveStatus activeStatus;

    /**
     * Reason for deactivation
     */
    private String activeStatusReason;

    /**
     * @see javax.persistence.Version
     */
    @Version
    private Integer version;

    private String source;

    @Column(unique = true, columnDefinition = "LONGTEXT")
    @JsonIgnore
    private String hash;


    @Transient
    @JsonInclude()
    private Set<Change<Serializable>> changes;

    @Transient
    @JsonIgnore
    private Map<String, String> division;

    @Transient
    @JsonIgnore
    private List<String> preProcessPipelineException;

    @Column(columnDefinition="bit(1) default 1")
    private boolean changed = true;

    public String getHash() {
        return hash;
    }

    @JsonIgnore
    public String hash() {
        return hash(new HashSet<>(), 0);
    }

    public boolean isCreate() {
        return isCreate;
    }

    public void setCreate(boolean create) {
        isCreate = create;
    }

    @Transient
    private boolean isCreate;

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

    private Stream<?> toItems(Object item, int level) {
        if (item instanceof Collection) {
            if (level > 1) {
                return Stream.of("");
            }
            return ((Collection<?>) item).stream();
        }
        return Stream.of(item);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> loadProperties() {
        Map<String, Object> props = JSONUtils.getObjectMapper().convertValue(this, Map.class);
        return filterProps(props);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> filterProps(Map<String, Object> props) {
        Map<String, Object> filteredProps = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            if (!EXCLUDED_PROPERTIES.contains(entry.getKey())) {
                Object value = entry.getValue();
                if (value instanceof Map) {
                    value = filterProps((Map<String, Object>) value);
                }
                if (value instanceof Collection) {
                    value = filterCollection((Collection<?>) value);
                }
                filteredProps.put(entry.getKey(), value);
            }
        }
        return filteredProps;
    }

    @SuppressWarnings({"unchecked"})
    private Object filterCollection(Collection<?> collection) {
        List<Object> items = new ArrayList<>();
        for (Object item : collection) {
            if (item instanceof Map) {
                item = filterProps((Map<String, Object>) item);
            }
            items.add(item);
        }
        return items;
    }

    public CommonDataModel setHash(String hash) {
        this.hash = hash;
        return this;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * Any extra attributes which are not part of the standard schema is stored here in json ofrmat
     */
    @Column(columnDefinition = "json")
    @Convert(converter = JSONObjectConverter.class)
    private JsonNode extendedAttributes;

    /**
     * @return the createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * @param createdBy the createdBy to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @return the modifiedBy
     */
    public String getModifiedBy() {
        return modifiedBy;
    }

    /**
     * @param modifiedBy the modifiedBy to set
     */
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    /**
     * @return the creationTime
     */
    public Date getCreationTime() {
        return creationTime;
    }

    /**
     * @param creationTime the creationTime to set
     */
    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * @return the lastModifiedTime
     */
    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }

    /**
     * @param lastModifiedTime the lastModifiedTime to set
     */
    public void setLastModifiedTime(Date lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    /**
     * @return the lob
     */
    public String getLob() {
        return lob;
    }

    /**
     * @param lob the lob to set
     */
    public void setLob(String lob) {
        this.lob = lob;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the activeStatus
     */
    public ActiveStatus getActiveStatus() {
        return activeStatus;
    }

    /**
     * @param activeStatus the activeStatus to set
     */
    public void setActiveStatus(ActiveStatus activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * @return the activeStatusReason
     */
    public String getActiveStatusReason() {
        return activeStatusReason;
    }

    /**
     * @param activeStatusReason the activeStatusReason to set
     */
    public void setActiveStatusReason(String activeStatusReason) {
        this.activeStatusReason = activeStatusReason;
    }

    /**
     * @return the extendedAttributes
     */
    public JsonNode getExtendedAttributes() {
        return extendedAttributes;
    }

    /**
     * @param extendedAttributes the extendedAttributes to set
     */
    public void setExtendedAttributes(JsonNode extendedAttributes) {
        this.extendedAttributes = extendedAttributes;
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : super.hashCode();
    }


    @JsonGetter
    public Set<Change<Serializable>> getChanges() {
        if(this.changes == null) {
            this.changes = findChanges();
        }
        return this.changes;
    }

    public Set<Change<Serializable>> findChanges() {
        return oldModel == null ? Collections.emptySet() : CdmDiffUtil.getChanges(this, this.getOldModel());
    }

    @JsonSetter
    public void setChanges(Set<Change<Serializable>> changes) {
        this.changes = changes;
    }

    @JsonIgnore
    @Transient
    public CommonDataModelService<?> getService() {

        return ServiceLocator.lookup((Class<CommonDataModel>) this.getClass());

    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CommonDataModel other = (CommonDataModel) obj;
        if (id == null) {
            return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @JsonIgnore
    @Transient
    public boolean canHash() {
        return false;
    }

    public boolean forceHash(){
        return false;
    }

    @JsonIgnore
    @Transient
    public CommonDataModelService<?> getService(Class clas) {

        return ServiceLocator.lookup((Class<CommonDataModel>) clas);

    }

    protected boolean checkEquals(Object o1, Object o2) {
        if (o1 == null) {
            if (o2 != null)
                return false;
        } else if (!o1.equals(o2)) {

            return false;
        }
        return true;
    }

    @JsonIgnore
    @Transient
    public boolean isActive() {
        return activeStatus != null && activeStatus.equals(ActiveStatus.ACTIVE);
    }

    /**
     * Compare objects if equals or not.
     *
     * @param <M> the generic type
     * @param obj2 the obj 2
     * @param ignoreId the ignore id
     * @return true, if successful
     * @throws IllegalArgumentException the illegal argument exception
     */
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
        return uniqueKeys.stream().allMatch(p -> Objects.equals(com.applicate.services.channelkart.utils.ReflectionUtils.readData(this, p),
                com.applicate.services.channelkart.utils.ReflectionUtils.readData(obj2, p)));
    }

    public Map<String, String> getDivision() {
        return division;
    }

    public void setDivision(Map<String, String> division) {
        this.division = division;
    }

    @JsonIgnore
    public CommonDataModel getOldModel() {
        return oldModel;
    }

    @JsonIgnore
    public CommonDataModel setOldModel(CommonDataModel oldModel) {
        this.oldModel = oldModel;
        // once we set hte model it's better to resetting the changes. Otherwise, it may hold the old changes
        setChanges(null);
        return this;
    }

    public boolean getChanged() {
        return changed;
    }

    public CommonDataModel setChanged(boolean changed) {
        this.changed = changed;
        return this;
    }

    @JsonIgnore
    @SuppressWarnings("unchecked")
    protected String extractUniqueKey() {
        Class<CommonDataModel> thisClass = (Class<CommonDataModel>) this.getClass();
        CommonDataModelService<CommonDataModel> service = ServiceLocator.lookup(thisClass);
        if (service != null) {
            return service.getKey(this);
        }
        return StringUtils.isNotEmpty(id) ? id : "";
    }

    /**
     * @return the preProcessPipelineException
     */
    public List<String> getPreProcessPipelineException() {
        return preProcessPipelineException;
    }

    /**
     * @param preProcessPipelineException the preProcessPipelineException to set
     */
    public void setPreProcessPipelineException(List<String> preProcessPipelineException) {
        this.preProcessPipelineException = preProcessPipelineException;
    }

    public void addPreProcessPipelineException(String stackTrace) {
        if(preProcessPipelineException==null) {
            this.preProcessPipelineException = new ArrayList<>();
        }
        preProcessPipelineException.add(stackTrace);
    }
}
