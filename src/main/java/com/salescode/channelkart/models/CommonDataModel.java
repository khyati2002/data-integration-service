package com.salescode.channelkart.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salescode.channelkart.converters.ActiveStatus;
import com.salescode.channelkart.converters.ActiveStatusConverter;
import com.salescode.channelkart.converters.DateToClientTimeZoneStringConverter;
import com.salescode.channelkart.converters.JSONObjectConverter;
import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.models.diff.Change;
import com.salescode.channelkart.utils.CdmDiffUtil;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.channelkart.utils.ReflectionUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

@MappedSuperclass
@Getter
@Setter
public class CommonDataModel implements Serializable {

   public static final Set<String> EXCLUDED_PROPERTIES = Set.of("hash", "forceHash", "isCreate", "id", "createdBy", "creationTime", "oldModel", "modifiedBy", "changes", "changed", "lastModifiedTime", "version", "lob","$jacocoData");

   /**
    * This is a Super Class of all Entity classes. All the Entities extend this class and the attributes are common for
    * the sub classes. These attributes are auto populated when the entity is saved.
    *
    * @see com.applicate.services.channelkart.services.AbstractCDMService#fillCommonAttributes(CommonDataModel) (T)
    */
   private static final long serialVersionUID = 1L;

   @JsonProperty(access = Access.READ_ONLY)
   private String createdBy;

   @Transient
   private transient CommonDataModel oldModel;

   @Transient
   @JsonIgnore
   private boolean forceHash;

   @JsonProperty(access = Access.READ_ONLY)
   private String modifiedBy;

   @JsonSerialize(converter = DateToClientTimeZoneStringConverter.class)
   @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
   @JsonProperty(access = Access.READ_ONLY)
   private Date creationTime;

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

   @Id
   @GeneratedValue(generator = "UUID")
//   @GenericGenerator(name = "UUID", strategy = "com.applicate.services.channelkart.services.UUIDIdentifier")
   private String id;

   @Convert(converter = ActiveStatusConverter.class)
   private ActiveStatus activeStatus;

   private String activeStatusReason;

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

   @JsonIgnore
   public String hash() {
      return hash(new HashSet<>(), 0);
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

   /**
    * Any extra attributes which are not part of the standard schema is stored here in json ofrmat
    */
   @Column(columnDefinition = "json")
   @Convert(converter = JSONObjectConverter.class)
   private JsonNode extendedAttributes;


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

//   @JsonIgnore
//   @Transient
//   public CommonDataModelService<?> getService() {
//
//      return ServiceLocator.lookup((Class<CommonDataModel>) this.getClass());
//
//   }

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

//   @JsonIgnore
//   @Transient
//   public CommonDataModelService<?> getService(Class clas) {
//
//      return ServiceLocator.lookup((Class<CommonDataModel>) clas);
//
//   }

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
        throw new CustomRuntimeException("not implemented");
//    	Set<String> uniqueKeys= EntityUtils.get().getUniqueKeys(obj2.getClass());
//    	uniqueKeys.add(id);
//    	if(ignoreId) {
//    		uniqueKeys.remove(id);
//    	}
//    	return uniqueKeys.stream().allMatch(p -> Objects.equals(ReflectionUtils.readData(this, p),
//    			ReflectionUtils.readData(obj2, p)));
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

//   @JsonIgnore
//   @SuppressWarnings("unchecked")
//   protected String extractUniqueKey() {
//      Class<CommonDataModel> thisClass = (Class<CommonDataModel>) this.getClass();
//      CommonDataModelService<CommonDataModel> service = ServiceLocator.lookup(thisClass);
//      if (service != null) {
//         return service.getKey(this);
//      }
//      return StringUtils.isNotEmpty(id) ? id : "";
//   }

   public void addPreProcessPipelineException(String stackTrace) {
		if(preProcessPipelineException==null) {
			this.preProcessPipelineException = new ArrayList<>();
		}
		preProcessPipelineException.add(stackTrace);
	}
}
