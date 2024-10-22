/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.models.EntityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The class EntityUtils.
 *
 * @author Manish Srivastava
 * @since  May 2020
 */

@SuppressWarnings("deprecation")
@Service
public class EntityUtils implements InitializingBean {

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(EntityUtils.class);

	/**
	 * The entity manager factory.
	 */
	@Autowired
	private EntityManagerFactory entityManagerFactory;

	/**
	 * The entitymanager.
	 */
	@PersistenceContext
	private EntityManager entitymanager;

//	@Autowired
//	private MetaDataService metaDataervice;
	private static final String GET_KEY_QUERY_DOMAIN_NAME = "cdmGetKeyQuery";
	private final ObjectMapper mapper = JSONUtils.getObjectMapper();
	private static Map<Class, Map<String, String>> nativeFieldsMap = new ConcurrentHashMap<>();
	private static final String id = "id";
	private static final List<Class> jsonNodeClassList = new ArrayList<>(Arrays.asList(JsonNode.class, ObjectNode.class));
	private Map<String, Class> entityClassMap = new ConcurrentHashMap<>();
	private Map<String, String> entityFieldMap = new ConcurrentHashMap<>();
	private Map<String, EntityInfo> entityInfoMap = new ConcurrentHashMap<>();
	private Map<String, Field> tableFieldsMap = new ConcurrentHashMap<>();
	private Map<String, Set<Field>> uniqueFieldsMap = new ConcurrentHashMap<>();
	private static Map<String, String> nativeTableNames = new HashMap<>();
	private static final Object lockObj = new Object();

	/**
	 * Gets the entity class.
	 *
	 * @param entityName the entity name
	 * @return the entity class
	 */
//	public Class getEntityClass(String entityName) {
//		if (entityClassMap.containsKey(entityName)) {
//			return entityClassMap.get(entityName);
//		}
//		for (EntityType<?> entity : entityManagerFactory.getMetamodel().getEntities()) {
//			if (entityName.equalsIgnoreCase(entity.getName())) {
//				entityClassMap.put(entityName, entity.getJavaType());
//				return entity.getJavaType();
//			}
//		}
//		throw new IllegalArgumentException(StringUtils.format("Entity {} not found", entityName));
//	}
//
//	/**
//	 * Gets the field.
//	 *
//	 * @param clazz       the clazz
//	 * @param entityField the entity field
//	 * @return the field
//	 */
//	public String getField(Class<?> clazz, String entityField) {
//		String key = clazz.getName() + ":" + entityField;
//		if (entityFieldMap.containsKey(key)) {
//			return entityFieldMap.get(key);
//		}
//		SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
//		AbstractEntityPersister persister = ((AbstractEntityPersister) sessionFactory.getClassMetadata(clazz));
//		String fieldName = null;
//		for (Field field : ReflectionUtils.getAllFields(clazz)) {
//			try {
//				String[] columnNames = persister.getPropertyColumnNames(field.getName());
//				if (columnNames.length > 0) {
//					String column = columnNames[0];
//					if (entityField.equals(column)) {
//						fieldName = field.getName();
//						break;
//					}
//				}
//			} catch (MappingException ex) {
//				if (logger.isDebugEnabled()) {
//					logger.debug(ex.getMessage());
//				}
//			}
//		}
//		entityFieldMap.put(key, fieldName);
//		return fieldName;
//
//	}
//
//
//	/**
//	 * Gets the table field.
//	 *
//	 * @return the field
//	 */
//	public EntityInfo getEntityInfo(String entityName) {
//		return getEntityInfo(getEntityClass(entityName));
//	}
//
//	/**
//	 * Gets the table field.
//	 *
//	 * @param clazz the clazz
//	 * @return the field
//	 */
//	public EntityInfo getEntityInfo(Class<?> clazz) {
//		if (entityInfoMap.containsKey(clazz.getName())) {
//			return entityInfoMap.get(clazz.getName());
//		}
//		SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
//		AbstractEntityPersister persister = ((AbstractEntityPersister) sessionFactory.getClassMetadata(clazz));
//		EntityInfo einfo = new EntityInfo();
//		einfo.setClassName(clazz.getName());
//		einfo.setTableName(getTableName(clazz));
//		Map<String, String> fieldMap = new LinkedHashMap<>();
//		Map<String, EntityFieldInfo> fieldInfoMap = new LinkedHashMap<>();
//
//		for (Field field : ReflectionUtils.getAllFields(clazz)) {
//			try {
//				String[] columnNames = persister.getPropertyColumnNames(field.getName());
//				if (columnNames.length > 0) {
//					String column = columnNames[0];
//					fieldMap.put(field.getName(), column);
//					fieldInfoMap.put(field.getName(),getFieldInfo(field,column));
//				}
//			} catch (MappingException ex) {
//				if (logger.isDebugEnabled()) {
//					logger.debug(ex.getMessage());
//				}
//			}
//		}
//		einfo.setFieldNameMap(fieldMap);
//		einfo.setFieldInfoMap(fieldInfoMap);
//		entityInfoMap.put(clazz.getName(), einfo);
//		return einfo;
//	}
//	public List<ObjectNode> fetchEntityInfo(String entityName){
//		MetaData metaAcceptConfig = metaDataervice.fetchByValue("entityInfoConfiguration","additionalMandatoryColumns");
//		Map <String,HashSet<String>> additionalMandatoryColumns = (metaAcceptConfig != null) ? JSONUtils.getObjectMapper().convertValue(metaAcceptConfig.getDomainValues().get(0),new TypeReference<HashMap<String,HashSet<String>>>() {}) : new HashMap<>();
//		return List.of(getEntityFieldsInfo(getEntityClass(entityName), additionalMandatoryColumns));
//	}
//	public ObjectNode getEntityFieldsInfo(Class<?> clazz, Map<String,HashSet<String>> additionalMandatoryColumns) {
//		ObjectNode node = JSONUtils.getObjectMapper().createObjectNode();
//		node.put("className", clazz.getName());
//		node.put("simpleClassName", clazz.getSimpleName());
//		ObjectNode fieldsInfo = node.putObject("fieldInfoMap");
//		for (Field f : ReflectionUtils.getAllFields(clazz)) {
//			if (CommonDataModel.EXCLUDED_PROPERTIES.contains(f.getName()))
//				continue;
//			ObjectNode ob = fieldsInfo.putObject(f.getName());
//			ob.put("name",f.getName());
//			ob.put("datatype",f.getType().getSimpleName());
//			ob.put("typeClass",f.getType().getName());
//			ob.put("isPrimitive",f.getType().isPrimitive());
//
//			ob.put("declaringClass",f.getDeclaringClass().getTypeName());
//			ob.put("isChannelkart",CommonDataModel.class.isAssignableFrom(f.getType()));
//
//			ob.put("jsonIgnore", f.getAnnotation(JsonIgnore.class)!=null);
//			ob.put("notNull", f.getAnnotation(NotNull.class)!=null);
//			ob.put("isMandatory",additionalMandatoryColumns.getOrDefault(clazz.getSimpleName(),new HashSet<>()).contains(f.getName()));
//
//			ob.put("genericType",JSONUtils.toJsonNode(f.getGenericType()));
//			ob.put("isCollection", Collection.class.isAssignableFrom(f.getType()) || Map.class.isAssignableFrom(f.getType()));
//		}
//		return node;
//	}
//
//	private EntityFieldInfo getFieldInfo(Field field,String columnName){
//		var finfo = new EntityFieldInfo();
//		finfo.setName(columnName);
//		finfo.setDataType(field.getType().getSimpleName());
//		finfo.setTypeClass(field.getType().getName());
//		finfo.setPrimitive(field.getType().isPrimitive());
//		return finfo;
//	}
//
//	/**
//	 * Gets the table field.
//	 *
//	 * @param clazz       the clazz
//	 * @param entityField the entity field
//	 * @return the field
//	 */
//	public String getTableField(Class<?> clazz, String entityField) {
//		if (nativeFieldsMap.containsKey(clazz)) {
//			Map<String, String> fields = nativeFieldsMap.get(clazz);
//			if (fields.containsKey(entityField)) {
//				return fields.get(entityField);
//			}
//		} else {
//			nativeFieldsMap.put(clazz, new HashMap<>());
//		}
//		String fieldName = getFieldName(clazz,entityField);
//		if (fieldName != null) {
//			nativeFieldsMap.get(clazz).put(entityField, fieldName);
//		}
//		return fieldName;
//
//	}
//
//	private String getFieldName(Class<?> clazz, String entityField){
//		SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
//		AbstractEntityPersister persister = ((AbstractEntityPersister) sessionFactory.getClassMetadata(clazz));
//		for (Field field : ReflectionUtils.getAllFields(clazz)) {
//			try {
//				if (entityField.equals(field.getName())) {
//					String[] columnNames = persister.getPropertyColumnNames(field.getName());
//					if (columnNames.length > 0) {
//						return columnNames[0];
//					}
//				}
//			} catch (MappingException ex) {
//				if (logger.isDebugEnabled()) {
//					logger.debug(ex.getMessage());
//				}
//			}
//		}
//		return null;
//	}
//
//	/**
//	 * Gets the field supported value.
//	 *
//	 * @param clazz       the clazz
//	 * @param entityField the entity field
//	 * @return the field
//	 */
//	public ArrayNode getSupportedValueMetadata(Class<?> clazz, String entityField) {
//		MetaDataService metaDataSevice = SpringContext.getBean(MetaDataService.class);
//		MetaData metaData = metaDataSevice.fetchByValue("supportedValues", clazz.getSimpleName() + "-" + entityField);
//		if (metaData != null) {
//			return metaData.getDomainValues();
//		}
//		return null;
//
//	}
//
//	public List<String> getSupportedValue(Class<?> clazz, String entityField) {
//		ArrayNode valueMetadata = getSupportedValueMetadata(clazz, entityField);
//		if (valueMetadata != null) {
//			return StreamSupport.stream(valueMetadata.spliterator(), false).map(s -> s.get("value").asText()).collect(Collectors.toList());
//		}
//		return List.of();
//
//	}
//
//
//	/**
//	 * Returns the table name for a given entity type in the {@link EntityManager}.
//	 *
//	 * @param <T>         the generic type
//	 * @param entityClass the entity class
//	 * @return the table name
//	 */
//	public <T> String getTableName(Class<T> entityClass) {
//		if (entityClass == null) {
//			return null;
//		}
//		String fullqname = entityClass.getName();
//		if (org.apache.commons.lang3.StringUtils.isBlank(fullqname)) {
//			return null;
//		}
//		if (nativeTableNames.containsKey(fullqname)) {
//			return nativeTableNames.get(fullqname);
//		}
//		Metamodel meta = entityManagerFactory.getMetamodel();
//		EntityType<T> entityType = meta.entity(entityClass);
//		Table t = entityClass.getAnnotation(Table.class);
//		String tableName = (t == null)
//				? entityType.getName().toUpperCase()
//				: t.name();
//		if (org.apache.commons.lang3.StringUtils.isNotBlank(tableName)) {
//			nativeTableNames.put(fullqname, tableName);
//		}
//		return tableName;
//	}
//
//	/**
//	 * Gets the class field.
//	 *
//	 * @param clazz      the clazz
//	 * @param tableField the table field
//	 * @return the class field
//	 */
//	public Field getClassField(Class<?> clazz, String tableField) {
//		String key = clazz.getName() + ":" + tableField;
//		if (tableFieldsMap.containsKey(key)) {
//			return tableFieldsMap.get(key);
//		}
//		SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
//		AbstractEntityPersister persister = ((AbstractEntityPersister) sessionFactory.getClassMetadata(clazz));
//		for (Field field : ReflectionUtils.getAllFields(clazz)) {
//			try {
//				String[] columnNames = persister.getPropertyColumnNames(field.getName());
//				if (columnNames.length > 0) {
//					String column = columnNames[0];
//					if (tableField.equals(column)) {
//						tableFieldsMap.put(key, field);
//						return field;
//					}
//				}
//			} catch (MappingException ex) {
//				if (logger.isDebugEnabled()) {
//					logger.debug(ex.getMessage());
//				}
//			}
//		}
//
//		return null;
//
//	}
//
//
//	public Field getClassFieldByName(Class<?> clazz, String entityFieldName) {
//		for (Field field : ReflectionUtils.getAllFields(clazz)) {
//			try {
//				if (field.getName().equalsIgnoreCase(entityFieldName)) {
//					return field;
//				}
//			} catch (Exception ex) {
//				if (logger.isDebugEnabled()) {
//					logger.debug(ex.getMessage());
//				}
//			}
//		}
//		return null;
//
//	}
//
//	/**
//	 * Gets the unique keys.
//	 *
//	 * @param clazz the clazz
//	 * @return the unique keys
//	 */
//	public Set<String> getUniqueKeys(Class<?> clazz) {
//		ArrayNode dynamicKeys = fetchDynamicPrimaryKeys(clazz.getSimpleName());
//		if (dynamicKeys.size() > 0) {
//			Set<String> uniquekeys = new HashSet<>();
//			dynamicKeys.forEach(dynamicKey -> {
//				String[] splitter = dynamicKey.asText().split("\\.");
//				if (splitter.length > 0) {
//					uniquekeys.add(splitter[0]);
//				} else uniquekeys.add(dynamicKey.asText());
//			});
//
//			return uniquekeys;
//		} else {
//			List<UniqueKeyContainer> containers = getUniqueKeyContainer(clazz);
//			Set<String> uniquekeys = new HashSet<>();
//			containers.forEach(action -> {
//				String[] splitter = action.getPath().split("\\.");
//				if (splitter.length > 0) {
//					uniquekeys.add(splitter[0]);
//				} else uniquekeys.add(action.getPath());
//			});
//			return uniquekeys;
//		}
//	}
//
//	/**
//	 * Gets the unique key container.
//	 *
//	 * @param clazz the clazz
//	 * @return the unique key container
//	 */
//	public List<UniqueKeyContainer> getUniqueKeyContainer(Class<?> clazz) {
//		Set<Field> uniquefields = findFields(clazz, UniqueKey.class);
//		List<UniqueKeyContainer> ukcontainer = new ArrayList<>();
//		uniquefields.forEach(key -> {
//			UniqueKeyContainer container = new UniqueKeyContainer();
//			String tablefield = Optional.ofNullable(getTableField(clazz, key.getName()))
//					.orElse(Arrays.asList(key.getAnnotationsByType(UniqueKey.class)).get(0).nativeName());
//			if (tablefield.equals("")) {
//				throw new IllegalStateException("tablefield cannot be null");
//			}
//			container.setNativeName(tablefield);
//			container.setField(key);
//			String path = Arrays.asList(key.getAnnotationsByType(UniqueKey.class)).get(0).path();
//			if (path.equals("")) path = key.getName();
//			container.setPath(path);
//			ukcontainer.add(container);
//		});
//		return ukcontainer;
//	}
//
//	/**
//	 * Gets the field value.
//	 *
//	 * @param clazz     the clazz
//	 * @param container the container
//	 * @param cdm       the cdm
//	 * @return the field value
//	 * @throws NoSuchFieldException     the no such field exception
//	 * @throws SecurityException        the security exception
//	 * @throws IllegalArgumentException the illegal argument exception
//	 * @throws IllegalAccessException   the illegal access exception
//	 */
//	public Object getFieldValue(Class<?> clazz, UniqueKeyContainer container, CommonDataModel cdm) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
//		Object val = cdm;
//		List<String> splitter = Arrays.asList(container.getPath().split("\\."));
//		if (splitter.size() > 1) {
//			try {
//				val = PropertyUtils.getNestedProperty(val, container.getPath());
//			} catch (NestedNullException nestex) {
//				if (logger.isDebugEnabled()) {
//					logger.debug("Nested value found null for '{}'", container.getPath());
//				}
//				return null;
//			} catch (InvocationTargetException | NoSuchMethodException e) {
//				logger.error("Exception: ", e);
//				return null;
//			}
//		} else {
//			Field field = findField(clazz, container.getPath());
//			field.setAccessible(true);
//			val = field.get(val);
//		}
//		return val;
//	}
//
//	/**
//	 * Gets the field value.
//	 *
//	 * @param path    the path
//	 * @param dataobj the dataobj
//	 * @return the field value
//	 * @throws NoSuchFieldException     the no such field exception
//	 * @throws SecurityException        the security exception
//	 * @throws IllegalArgumentException the illegal argument exception
//	 * @throws IllegalAccessException   the illegal access exception
//	 */
//	public Object getFieldValue(String path, CommonDataModel dataobj) {
//		Class<?> clazz = dataobj.getClass();
//		UniqueKeyContainer container = new UniqueKeyContainer();
//		container.setPath(path);
//		try {
//			return getFieldValue(clazz, container, dataobj);
//		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
//			throw new CustomRuntimeException(e, e.getLocalizedMessage());
//		}
//	}
//
//	/**
//	 * Find unique records.
//	 *
//	 * @param clazz    the clazz
//	 * @param elements the elements
//	 * @return the list
//	 */
//	public List<CommonDataModel> findUniqueRecords(Class<?> clazz, List<CommonDataModel> elements) {
//		try {
//
//			String tablename = getTableName(clazz);
//			List<UniqueKeyContainer> ukcontainer = getUniqueKeyContainer(clazz);
//			if (ukcontainer != null && !ukcontainer.isEmpty()) {
//				StringBuilder buffer1 = new StringBuilder(getBaseSQL(tablename));
//				StringBuilder buffer2 = new StringBuilder();
//				Iterator<CommonDataModel> cdmiter = elements.iterator();
//				while (cdmiter.hasNext()) {
//					CommonDataModel element = cdmiter.next();
//					if (buffer2.length() > 0) {
//						buffer2.append(" or ");
//					}
//					StringBuilder buffer3 = new StringBuilder();
//					for (UniqueKeyContainer container : ukcontainer) {
//						Object value = getFieldValue(clazz, container, element);
//						fillBuffer(element,container,value,buffer3);
//					}
//					buffer2.append(buffer3);
//				}
//				buffer1.append(buffer2);
//				Query sqlquery = entitymanager.createNativeQuery(buffer1.toString(), clazz);
//				return sqlquery.getResultList();
//			} else {
//				return new ArrayList<>();
//			}
//
//		} catch (Exception ex) {
//			logger.error("Error while fetching unique columns. Please check @UniqueKey configuration", ex);
//		}
//		return List.of();
//
//	}
//
//	private void fillBuffer(CommonDataModel element,UniqueKeyContainer container,Object value,StringBuilder buffer3)
//			throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
//		try {
//			StringBuilder buffer4 = new StringBuilder();
//			if (PropertyUtils.getPropertyType(element, container.getPath()) == String.class) {
//				buffer4.append("'").append(value).append("'");
//			} else {
//				buffer4.append(value);
//			}
//			if (!buffer4.toString().isEmpty()) {
//				if (buffer3.length() > 0) {
//					buffer3.append(" and ");
//				}
//				buffer3.append(container.getNativeName()).append("=").append(buffer4);
//			}
//		} catch (NestedNullException nestedex) {
//			logger.debug("'{}' found null for unique key generation", container.getPath());
//		}
//	}
//
//	private String getBaseSQL(String tableName){
//		return "select * from " + tableName + " where ";
//	}
//
//	public List<CommonDataModel> findUniqueRecord(Class<?> clazz, List<CommonDataModel> elements, ArrayNode columnArr) {
//		String tablename = getTableName(clazz);
//		StringBuilder buffer1 = new StringBuilder(getBaseSQL(tablename));
//		StringBuilder buffer2 = new StringBuilder();
//		Iterator<CommonDataModel> cdmiter = elements.iterator();
//		while (cdmiter.hasNext()) {
//			CommonDataModel element = cdmiter.next();
//			if (buffer2.length() > 0) {
//				buffer2.append(" or ");
//			}
//			StringBuilder value = new StringBuilder();
//			for (int i = 0; i < columnArr.size(); i++) {
//				value.append(getBeanProperty(element, columnArr.get(i).asText()));
//			}
//			buffer2.append("id").append("=").append("'").append(value).append("'");
//		}
//		buffer1.append(buffer2);
//		Query sqlquery = entitymanager.createNativeQuery(buffer1.toString(), clazz);
//		return sqlquery.getResultList();
//	}
//
//	public List<CommonDataModel> findUniqueRecordIn(Class<?> clazz, List<CommonDataModel> elements, ArrayNode columnArr) {
//		String tablename = getTableName(clazz);
//		StringBuilder buffer1 = new StringBuilder(getBaseSQL(tablename));
//		StringBuilder buffer2 = new StringBuilder();
//		Iterator<CommonDataModel> cdmiter = elements.iterator();
//		while (cdmiter.hasNext()) {
//			CommonDataModel element = cdmiter.next();
//			if (buffer2.length() > 0) {
//				buffer2.append(",");
//			} else {
//				buffer2.append(" id in ( ");
//			}
//			String value = "";
//			for (int i = 0; i < columnArr.size(); i++) {
//				String tempVal = String.valueOf(getBeanProperty(element, columnArr.get(i).asText()));
//				if (tempVal != null) {
//					if (i > 0) {
//						value = new StringBuilder(value).append("-").append(tempVal.toLowerCase()).toString();
//					} else {
//						value = new StringBuilder(value).append(tempVal.toLowerCase()).toString();
//					}
//				}
//			}
//			buffer2.append("'").append(StringUtils.escapeSql(value.replace(" ", "-"))).append("'");
//		}
//		buffer1.append(buffer2.append(" ) "));
//		Query sqlquery = entitymanager.createNativeQuery(buffer1.toString(), clazz);
//		return sqlquery.getResultList();
//	}
//
//	public CommonDataModel findUniqueRecord(Class<?> clazz, CommonDataModel element, ArrayNode columnArr) {
//		String tablename = getTableName(clazz);
//		StringBuilder buffer1 = new StringBuilder(getBaseSQL(tablename));
//		StringBuilder buffer2 = new StringBuilder();
//
//		String value = "";
//		for (int i = 0; i < columnArr.size(); i++) {
//			String tempval = String.valueOf(getBeanProperty(element, columnArr.get(i).asText()));
//			if (tempval != null) {
//				if (value.isBlank()) {
//					value = tempval.toLowerCase();
//				} else {
//					value = new StringBuilder(value).append("-").append(tempval.toLowerCase()).toString();
//				}
//				value = value.replace(" ", "-");
//			}
//		}
//		buffer2.append("id").append("=").append("'").append(checkGenerateMD5Hash(clazz.getSimpleName()) ? StringUtils.escapeSql(LOBCommand.getMd5(value)) : StringUtils.escapeSql(value)).append("'");
//		buffer1.append(buffer2);
//		Query sqlquery = entitymanager.createNativeQuery(buffer1.toString(), clazz);
//		try {
//			return (CommonDataModel) sqlquery.getSingleResult();
//		} catch (NoResultException nr) {
//			return null;
//		}
//	}
//
//	public CommonDataModel findRecords(Class<?> clazz, CommonDataModel element) {
//		ArrayNode dynamicPrimaryKeys = fetchDynamicPrimaryKeys(clazz.getSimpleName());
//		if (dynamicPrimaryKeys.size() > 0) {
//			return findUniqueRecord(clazz, element, dynamicPrimaryKeys);
//		} else {
//			return findUniqueRecord(clazz, element);
//		}
//	}
//
//	public List<CommonDataModel> findRecords(Class<?> clazz, List<CommonDataModel> element) {
//		ArrayNode dynamicPrimaryKeys = fetchDynamicPrimaryKeys(clazz.getSimpleName());
//		if (dynamicPrimaryKeys.size() > 0) {
//			return findUniqueRecordIn(clazz, element, dynamicPrimaryKeys);
//		} else {
//			return findUniqueRecords(clazz, element);
//		}
//	}
//
//	public CommonDataModel findUniqueRecord(Class<?> clazz, CommonDataModel element) {
//		try {
//			String tablename = getTableName(clazz);
//			List<UniqueKeyContainer> ukcontainer = getUniqueKeyContainer(clazz);
//			if (ukcontainer.isEmpty()) {
//				UniqueKeyContainer uk = new UniqueKeyContainer();
//				uk.setNativeName("id");
//				uk.setPath("id");
//				ukcontainer.add(uk);
//			}
//			StringBuilder buffer1 = new StringBuilder(getBaseSQL(tablename));
//			StringBuilder buffer2 = new StringBuilder();
//			if (buffer2.length() > 0) {
//				buffer2.append(" or ");
//			}
//			StringBuilder buffer3 = new StringBuilder();
//			for (UniqueKeyContainer container : ukcontainer) {
//				Object value = getFieldValue(clazz, container, element);
//				fillTempbuffer(element,container,value,buffer3);
//			}
//			buffer2.append(buffer3);
//			buffer1.append(buffer2);
//			Query sqlquery = entitymanager.createNativeQuery(buffer1.toString(), clazz);
//			return execute(sqlquery);
//		}
//		catch(NoResultException nex) {
//			return null;
//		}
//		catch (Exception ex) {
//			throw new UnexpectedResultException(ex,"Error while fetching unique records. Reason : {}", ex.getMessage());
//		}
//
//	}
//
//	private CommonDataModel execute(Query query) {
//		try {
//			return (CommonDataModel) query.getSingleResult();
//		} catch (NoResultException e) {
//			// we don't need to print this exception. If the query is not matching any record then this exception will throw.
//			// The caller is expecting null as return type, so it's better to ignore this exception.
//			return null;
//		}
//	}
//
//	private void fillTempbuffer(CommonDataModel element,UniqueKeyContainer container,Object value,StringBuilder buffer3)
//			throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
//		try {
//			StringBuilder buffer4 = new StringBuilder();
//			Class<?> propertyType = PropertyUtils.getPropertyType(element, container.getPath());
//			if (propertyType == String.class || propertyType.isEnum()) {
//				buffer4.append( generateConditionFromValue(value) );
//			} else {
//				buffer4.append(value);
//			}
//			if (!buffer4.toString().isEmpty()) {
//				if (buffer3.length() > 0) {
//					buffer3.append(" and ");
//				}
//				if (value != null) {
//					buffer3.append(container.getNativeName()).append("=").append(buffer4);
//				} else {
//					buffer3.append(container.getNativeName()).append(buffer4);
//				}
//			}
//		} catch (NestedNullException nestedex) {
//			logger.debug("'{}' found null for unique key generation", container.getPath());
//		}
//	}
//
//	/**
//	 * Generate StringBuilder object with condition value.
//	 * <p>
//	 *     If the value passed is null, product "is null" condition.
//	 * </p>
//	 * @param value
//	 * @return
//	 */
//	private StringBuilder generateConditionFromValue(Object value){
//		StringBuilder result = new StringBuilder();
//		if (value == null) {
//			result.append(" is null ");
//		} else if (String.valueOf(value).contains("'")) {
//			result.append("\"").append(value).append("\"");
//		} else {
//			result.append("'").append(value).append("'");
//		}
//		return result;
//	}
//
//	/**
//	 * Generate unique key hash code.
//	 *
//	 * @param cdm        the cdm
//	 * @param entityname the entityname
//	 * @return the int
//	 */
//	public int generateUniqueKeyHashCode(CommonDataModel cdm, String entityname) {
//		int hashid = Integer.MAX_VALUE;
//		try {
//			Class<?> clazz = getEntityClass(entityname);
//			List<UniqueKeyContainer> containers = getUniqueKeyContainer(clazz);
//			for (UniqueKeyContainer key : containers) {
//				Object elem = getFieldValue(clazz, key, cdm);
//				if (elem != null) {
//					hashid += elem.hashCode();
//				}
//			}
//		} catch (Exception ex) {
//			logger.error("Error while generating uniquekeyhash", ex);
//		}
//		return hashid;
//	}
//
//	/**
//	 * Find fields.
//	 *
//	 * @param classs the classs
//	 * @param ann    the ann
//	 * @return the sets the
//	 */
//	private Set<Field> findFields(Class<?> classs, Class<? extends Annotation> ann) {
//		String key = classs.getName() + ":" + ann.getName();
//		if (uniqueFieldsMap.containsKey(key)) {
//			return uniqueFieldsMap.get(key);
//		}
//		Set<Field> set = new HashSet<>();
//		Class<?> c = classs;
//		while (c != null) {
//			for (Field field : ReflectionUtils.getAllFields(c)) {
//				if (field.isAnnotationPresent(ann)) {
//					set.add(field);
//				}
//			}
//			c = c.getSuperclass();
//		}
//		uniqueFieldsMap.put(key, set);
//		return set;
//	}
//
//	public <T> List<?> findDataByQuery(Class<T> clazz, String query, boolean isNative) {
//		if (NullUtils.isNotNull(query)) {
//			List<T> data = null;
//			if (isNative) {
//				Query q = null;
//				if (clazz == List.class) {
//					q = entitymanager.createNativeQuery(query);
//				} else if (clazz == Map.class) {
//					q = entitymanager.createNativeQuery(query);
//					q.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
//				} else {
//					q = entitymanager.createNativeQuery(query, clazz);
//				}
//				q.setHint(QueryHints.NATIVE_LOCKMODE, LockModeType.NONE);
//				data = q.getResultList();
//			} else {
//				TypedQuery<T> q = entitymanager.createQuery(query, clazz);
//				data = q.getResultList();
//			}
//			return data;
//		}
//		return List.of();
//	}
//
//	/**
//	 * The instance.
//	 */
	private static EntityUtils instance;
//
//	/**
//	 * After properties set.
//	 *
//	 * @throws Exception the exception
//	 */
	@Override
	public void afterPropertiesSet() {
		setInstance(this);
	}

	private static synchronized void setInstance(EntityUtils e) {
		instance = e;
	}

	/**
	 * Gets the.
	 *
	 * @return the entity utils
	 */
	public static EntityUtils get() {
		return instance;
	}


//	public String generateId(CommonDataModel cdm) {
//		return generateId(cdm, false);
//	}
//
//	public String getBeanProperty(Object cdm, String property) {
//		try {
//			String[] field = property.split("[.]");
//			BeanWrapper source = new BeanWrapperImpl(cdm);
//			Class<?> propClass = source.getPropertyType(field[0]);
//			if (propClass != null && JsonNode.class.isAssignableFrom(propClass) && jsonNodeClassList.contains(propClass)) {
//				if (field.length == 2) {
//					JsonNode obj = (JsonNode) source.getPropertyValue(field[0]);
//					return (obj != null && obj.has(field[1])) ? obj.get(field[1]).asText() : null;
//				} else if (field.length < 2)
//					throw new IllegalArgumentException("Key is not defied for json node {}'", property);
//				else
//					throw new InvalidFieldException("Nested Json Key [{}] is not supported in dynamic key creation.", property);
//			}
//				return BeanUtils.getProperty(cdm, property);
//
//		} catch (Exception e) {
//			if (logger.isDebugEnabled()) {
//				logger.debug("could not find property {} from cdm object {}, class {}", property, cdm, cdm.getClass());
//			}
//			logger.error(e.getMessage());
//			if (e instanceof CustomRuntimeException)
//				throw new CustomRuntimeException(e);
//			return "";
//		}
//	}
//
//	public ArrayNode fetchDynamicPrimaryKeys(String entityName) {
//		MetaDataService metaDataSevice = SpringContext.getBean(MetaDataService.class);
//		MetaData metaData = metaDataSevice.fetchByValue(entityName, "DynamicUniqueKey");
//		ArrayNode columnArr = JSONUtils.getObjectMapper().createArrayNode();
//		if (metaData != null) {
//			columnArr = (ArrayNode) metaData.getDomainValues().get(0).get("dynamicKeys");
//		}
//		return columnArr;
//	}
//
//	public static String[] getNullPropertyNames(Object source) {
//		final BeanWrapper src = new BeanWrapperImpl(source);
//		java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();
//
//		Set<String> emptyNames = new HashSet<>();
//		for (java.beans.PropertyDescriptor pd : pds) {
//			Object srcValue = src.getPropertyValue(pd.getName());
//			if (srcValue == null || isNullNode(srcValue)) emptyNames.add(pd.getName());
//		}
//		String[] result = new String[emptyNames.size()];
//		return emptyNames.toArray(result);
//	}
//
//	public static void copyProperties(Object src, Object tgt) {
//		synchronized (lockObj) {
//			/*BeanWrapper source = new BeanWrapperImpl(src);
//			BeanWrapper target = new BeanWrapperImpl(tgt);
//			Set<String> jsonFields = new HashSet<String>();
//			java.beans.PropertyDescriptor[] pdsrc = source.getPropertyDescriptors();
//			for(java.beans.PropertyDescriptor pd:pdsrc) {
//				if(JsonNode.class.isAssignableFrom(pd.getPropertyType()) && NullUtils.isNotNull(source.getPropertyValue(pd.getName()))){
//					jsonFields.add(pd.getName());
//					if(NullUtils.isNotNull(target.getPropertyValue(pd.getName()))){
//						JsonNode mergedJson = null;
//						try {
//							mergedJson = JSONUtils.mergeJsonNodes((JsonNode)source.getPropertyValue(pd.getName()), (JsonNode)target.getPropertyValue(pd.getName()));
//						} catch (IOException e) {
//							log.error("stacktrace", e);
//						}
//						target.setPropertyValue(pd.getName(), mergedJson);
//					}else {
//						target.setPropertyValue(pd.getName(), source.getPropertyValue(pd.getName()));
//					}
//
//				}
//			}
//			tgt=target.getWrappedInstance();*/
//			copyProperties(src, tgt, (String)null);
//		}
//	}
//
//	public static void copyProperties(Object src, Object tgt, String... strings) {
//		synchronized (lockObj) {
//			String[] data = getNullPropertyNames(src);
//			Set<String> fields = new HashSet<>(Arrays.asList(data));
//			if(strings!=null) {
//				fields.addAll(
//						Arrays.asList(strings).stream().filter(Objects::nonNull).collect(Collectors.toList()));
//			}
//			BeanWrapper source = new BeanWrapperImpl(src);
//			BeanWrapper target = new BeanWrapperImpl(tgt);
//			java.beans.PropertyDescriptor[] pdsrc = source.getPropertyDescriptors();
//			for (java.beans.PropertyDescriptor pd : pdsrc) {
//				Object propertyValue= source.getPropertyValue(pd.getName());
//				if (propertyValue != null && JsonNode.class.isAssignableFrom(pd.getPropertyType()) && !fields.contains(pd.getName()) && jsonNodeClassList.contains(propertyValue.getClass())) {
//					fields.add(pd.getName());
//					if (target.getPropertyValue(pd.getName()) == null || isNullNode(target.getPropertyValue(pd.getName()))) {
//						target.setPropertyValue(pd.getName(), source.getPropertyValue(pd.getName()));
//					} else {
//						JsonNode mergedJson = null;
//						try {
//							mergedJson = JSONUtils.mergeJsonNodes((JsonNode) source.getPropertyValue(pd.getName()), (JsonNode) target.getPropertyValue(pd.getName()));
//						} catch (IOException e) {
//							logger.error("stacktrace", e);
//						}
//						target.setPropertyValue(pd.getName(), mergedJson);
//					}
//				}
//			}
//			tgt = target.getWrappedInstance();
//			org.springframework.beans.BeanUtils.copyProperties(src, tgt, fields.toArray(new String[0]));
//		}
//	}
//
//	public Field findField(Class<?> clazz, String fieldName) {
//		Class<?> c = clazz;
//		Field tempfield = null;
//		while (c != null) {
//			for (Field field : ReflectionUtils.getAllFields(c)) {
//				if (field.getName().equals(fieldName)) {
//					tempfield = field;
//					break;
//				}
//			}
//			c = c.getSuperclass();
//		}
//		return tempfield;
//	}
//
//	@Transactional(propagation = Propagation.REQUIRED)
//	public <T> Integer updateByQuery(Class<T> clazz, String query, boolean isNative) {
//		if (NullUtils.isNotNull(query)) {
//			if (isNative) {
//				Query q = entitymanager.createNativeQuery(query);
//				q.setHint(QueryHints.NATIVE_LOCKMODE, LockModeType.NONE);
//				return q.executeUpdate();
//			} else {
//				TypedQuery<T> q = entitymanager.createQuery(query, clazz);
//				return q.executeUpdate();
//			}
//		}
//		throw new ExecutionInteruptedException("Invalid query found : {}. Please check.");
//	}
//
	public static <T> T deepClone(T src) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(src);

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bais);
			return (T) ois.readObject();
		} catch (Exception e) {
			throw new CustomRuntimeException("Could not clone object:" + src);
		}
	}
//
//	/**
//	 * Compare objects if equals or not.
//	 *
//	 * @param <M>      the generic type
//	 * @param obj1     the obj 1
//	 * @param obj2     the obj 2
//	 * @param ignoreId the ignore id
//	 * @return true, if successful
//	 * @throws IllegalArgumentException the illegal argument exception
//	 */
//	public <M extends CommonDataModel> boolean compare(final M obj1, final M obj2, boolean ignoreId) throws CustomRuntimeException {
//		try {
//			Assert.notNull(obj1, "Comparing parameter 1");
//			Assert.notNull(obj2, "Comparing parameter 2");
//			if (obj1.getClass() != obj2.getClass()) {
//				throw new IllegalArgumentException("Comparing objects must belong to same class. Class 1 : " + obj1.getClass().getName()
//						+ ", Class 2 : " + obj2.getClass().getName());
//			}
//		} catch (IllegalArgumentException ex) {
//			throw new CustomRuntimeException(ex);
//		}
//		Set<String> uniqueKeys = getUniqueKeys(obj1.getClass());
//		uniqueKeys.add(id);
//		if (ignoreId) {
//			uniqueKeys.remove(id);
//		}
//		return uniqueKeys.stream().allMatch(p -> Objects.equals(com.applicate.services.channelkart.utils.ReflectionUtils.readData(obj1, p),
//				com.applicate.services.channelkart.utils.ReflectionUtils.readData(obj2, p)));
//	}
//
//	public static boolean isNullNode(Object value) {
//		return value instanceof JsonNode && ((JsonNode) value).isNull();
//	}
//
//	/**
//	 * it will check in metadata for the dynamic keys for that entityName if there is a flag "generateHash" and it is true
//	 * if it is true then a hash for the dynamic_id is generated
//	 *
//	 * @param entityName
//	 * @return
//	 */
//	private boolean checkGenerateMD5Hash(String entityName) {
//		MetaDataService metaDataService = SpringContext.getBean(MetaDataService.class);
//		MetaData metaData = metaDataService.fetchByValue(entityName, "DynamicUniqueKey");
//
//		boolean generateHash = false;
//		if (metaData != null && metaData.getDomainValues()!=null) {
//			JsonNode dynamicKeysNode = metaData.getDomainValues().get(0);
//			if (dynamicKeysNode != null) {
//				generateHash = dynamicKeysNode.has("generateHash") && dynamicKeysNode.get("generateHash").asBoolean();
//			}
//		}
//		return generateHash;
//	}
//
//	@SuppressWarnings("all")
//	public String generateId(CommonDataModel cdm, boolean findByDynamicKeyOnly) {
//		String genratedId = "";
//		String entityName = cdm.getClass().getSimpleName();
//		ArrayNode columnArr = fetchDynamicPrimaryKeys(entityName);
//		boolean generateHash = checkGenerateMD5Hash(entityName);
//		if (columnArr.size() != 0) {
//			for (int i = 0; i < columnArr.size(); i++) {
//				String columnName = columnArr.get(i).asText();
//				String value = String.valueOf(getBeanProperty(cdm, columnName));
//				if (value != null) {
//					if (genratedId.isBlank()) {
//						genratedId = value.toLowerCase();
//					} else {
//						genratedId = new StringBuffer(genratedId).append("-").append(value.toLowerCase()).toString();
//					}
//					genratedId = genratedId.replace(" ", "-");
//				}
//			}
//		} else if (findByDynamicKeyOnly) {
//			throw new ValidationFailedException("dynamic key doesnot exist");
//		} else {
//			genratedId = UUID.randomUUID().toString();
//		}
//		if(generateHash){
//			return genratedId.equals("") ? UUID.randomUUID().toString() : LOBCommand.getMd5(genratedId);
//		}
//		return genratedId.equals("") ? UUID.randomUUID().toString() : genratedId;
//	}
//
//	public <T extends CommonDataModel> List<T> getGetKeyId(T cdmObject) {
//		String cdmClassName = cdmObject.getClass().getSimpleName();
//		MetaData metaConfig = metaDataervice.fetchByValue(GET_KEY_QUERY_DOMAIN_NAME, cdmClassName);
//		if (metaConfig != null && metaConfig.getDomainValues().get(0).has("query")) {
//			String sql = metaConfig.getDomainValues().get(0).get("query").asText();
//			Map<String, Object> params = mapper.convertValue(cdmObject, new TypeReference<>() {
//			});
//			String finalQuery = replaceDynamicKeys(sql, params);
//			Query query = entitymanager.createQuery(finalQuery);
//			return query.getResultList();
//
//		} else {
//			return new ArrayList<>(1);
//		}
//	}
//
//	public String replaceDynamicKeys(String string, Map<String, Object> params) {
//		TemplateEngine templateEngine = SpringContext.getBean(TemplateEngine.class);
//		return templateEngine.applyInline(string, params);
//	}
//
//	public static String getDataChanges(Set<Change<Serializable>> dataChanges){
//		List<String> ignoreList = new ArrayList<>();
//		ignoreList.add("lastModifiedTime");
//		ignoreList.add("modifiedBy");
//		ignoreList.add("creationTime");
//		ignoreList.add("createdBy");
//		ignoreList.add("version");
//		ignoreList.add("hash");
//		StringBuilder changes = new StringBuilder();
//		for(Change<?> ch: dataChanges){
//			if(!ignoreList.contains(ch.getName())){
//				changes.append(ch);
//				changes.append("\n");
//			}
//		}
//
//		return changes.toString();
//	}
}
