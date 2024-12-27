package com.salescode.channelkart.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.channelkart.annotation.UniqueKey;
import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.models.MetaData;
import com.salescode.channelkart.pojo.UniqueKeyContainer;
import com.salescode.channelkart.services.MetaDataService;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.templates.TemplateEngine;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.MappingException;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.QueryHints;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.*;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.salescode.channelkart.utils.EntityInfo.*;

@Service
public class EntityUtils implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(EntityUtils.class);
    private static final String GET_KEY_QUERY_DOMAIN_NAME = "cdmGetKeyQuery";
    private static final List<Class> jsonNodeClassList = new ArrayList<>(Arrays.asList(JsonNode.class, ObjectNode.class));
    private static final Map<String, Class<? extends CommonDataModel>> entityClassMap = new ConcurrentHashMap<>();
    private static final Map<String, String> nativeTableNames = new HashMap<>();
    private static final Object lockObj = new Object();
    private static Map<Class, Map<String, String>> nativeFieldsMap = new ConcurrentHashMap<>();
    private Map<String, EntityInfo> entityInfoMap = new ConcurrentHashMap<>();
    private static EntityUtils instance;
    private final ObjectMapper mapper = JSONUtils.getObjectMapper();
    Set<Class<? extends CommonDataModel>> subClasses;
    @Autowired
    MetaDataService metaDataService;
    private Map<String, Set<Field>> uniqueFieldsMap = new ConcurrentHashMap<>();
    private Map<String, Field> tableFieldsMap = new ConcurrentHashMap<>();
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @PersistenceContext
    private EntityManager entitymanager;
    @Autowired
    private MetaDataService metaDataervice;


    public EntityUtils() {
        subClasses = ReflectionUtils.findSubClasses(CommonDataModel.class);
    }

    public static <T> T deepClone(T src) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(src);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (T) ois.readObject();
        } catch (Exception e) {
            //    throw new ConverterException("Could not clone object:" + src);
        }
        return null;
    }

    public static boolean isNullNode(Object value) {
        return value instanceof JsonNode && ((JsonNode) value).isNull();
    }

    public static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null || isNullNode(srcValue)) emptyNames.add(pd.getName());
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    public static void copyProperties(Object src, Object tgt) {
        synchronized (lockObj) {
            String[] data = getNullPropertyNames(src);
            Set<String> fields = new HashSet<>(Arrays.asList(data));
            BeanWrapper source = new BeanWrapperImpl(src);
            BeanWrapper target = new BeanWrapperImpl(tgt);
            java.beans.PropertyDescriptor[] pdsrc = source.getPropertyDescriptors();
            for (java.beans.PropertyDescriptor pd : pdsrc) {
                Object propertyValue = source.getPropertyValue(pd.getName());
                if (propertyValue != null && JsonNode.class.isAssignableFrom(pd.getPropertyType()) && !fields.contains(pd.getName()) && jsonNodeClassList.contains(propertyValue.getClass())) {
                    fields.add(pd.getName());
                    if (target.getPropertyValue(pd.getName()) == null || isNullNode(target.getPropertyValue(pd.getName()))) {
                        target.setPropertyValue(pd.getName(), source.getPropertyValue(pd.getName()));
                    } else {
                        JsonNode mergedJson = null;
                        try {
                            mergedJson = JSONUtils.mergeJsonNodes((JsonNode) source.getPropertyValue(pd.getName()), (JsonNode) target.getPropertyValue(pd.getName()));
                        } catch (IOException e) {
                            logger.error("stacktrace", e);
                        }
                        target.setPropertyValue(pd.getName(), mergedJson);
                    }
                }
            }
            tgt = target.getWrappedInstance();
            org.springframework.beans.BeanUtils.copyProperties(src, tgt, fields.toArray(new String[0]));
        }
    }

    public static void copyProperties(Object src, Object tgt, String... strings) {
        synchronized (lockObj) {
            String[] data = getNullPropertyNames(src);
            Set<String> fields = new HashSet<>(Arrays.asList(data));
            if (strings != null) {
                fields.addAll(
                        Arrays.asList(strings).stream().filter(Objects::nonNull).collect(Collectors.toList()));
            }
            BeanWrapper source = new BeanWrapperImpl(src);
            BeanWrapper target = new BeanWrapperImpl(tgt);
            java.beans.PropertyDescriptor[] pdsrc = source.getPropertyDescriptors();
            for (java.beans.PropertyDescriptor pd : pdsrc) {
                Object propertyValue = source.getPropertyValue(pd.getName());
                if (propertyValue != null && JsonNode.class.isAssignableFrom(pd.getPropertyType()) && !fields.contains(pd.getName()) && jsonNodeClassList.contains(propertyValue.getClass())) {
                    fields.add(pd.getName());
                    if (target.getPropertyValue(pd.getName()) == null || isNullNode(target.getPropertyValue(pd.getName()))) {
                        target.setPropertyValue(pd.getName(), source.getPropertyValue(pd.getName()));
                    } else {
                        JsonNode mergedJson = null;
                        try {
                            mergedJson = JSONUtils.mergeJsonNodes((JsonNode) source.getPropertyValue(pd.getName()), (JsonNode) target.getPropertyValue(pd.getName()));
                        } catch (IOException e) {
                            logger.error("stacktrace", e);
                        }
                        target.setPropertyValue(pd.getName(), mergedJson);
                    }
                }
            }
            tgt = target.getWrappedInstance();
            org.springframework.beans.BeanUtils.copyProperties(src, tgt, fields.toArray(new String[0]));
        }
    }

    public static EntityUtils get() {
        return instance;
    }

    private static synchronized void setInstance(EntityUtils e) {
        instance = e;
    }

    public String replaceDynamicKeys(String string, Map<String, Object> params) {
        TemplateEngine templateEngine = SpringContext.getBean(TemplateEngine.class);
        return templateEngine.applyInline(string, params);
    }

    public Class<? extends CommonDataModel> getEntityClass(String entityName) {
        if (entityClassMap.containsKey(entityName)) {
            return entityClassMap.get(entityName);
        }
        for (var entity : subClasses) {
            if (entityName.equalsIgnoreCase(entity.getSimpleName())) {
                entityClassMap.put(entityName, entity);
                return entity;
            }
        }
        throw new IllegalArgumentException("Entity not found: " + entityName);
    }

    public String getBeanProperty(Object cdm, String property) {
        try {
            String[] field = property.split("[.]");
            BeanWrapper source = new BeanWrapperImpl(cdm);
            Class<?> propClass = source.getPropertyType(field[0]);
            if (propClass != null && JsonNode.class.isAssignableFrom(propClass) && jsonNodeClassList.contains(propClass)) {
                if (field.length == 2) {
                    JsonNode obj = (JsonNode) source.getPropertyValue(field[0]);
                    return (obj != null && obj.has(field[1])) ? obj.get(field[1]).asText() : null;
                } else if (field.length < 2) {
                    //throw new IllegalArgumentException("Key is not defied for json node {}'", property);
                } else {
                    // throw new InvalidFieldException("Nested Json Key [{}] is not supported in dynamic key creation.", property);
                }
            }
            return BeanUtils.getProperty(cdm, property);

        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("could not find property {} from cdm object {}, class {}", property, cdm, cdm.getClass());
            }
            logger.error(e.getMessage());
            if (e instanceof CustomRuntimeException)
                throw new CustomRuntimeException(e);
            return "";
        }
    }

    public String generateId(CommonDataModel cdm) {
        return generateId(cdm, false);
    }

    private String getBaseSQL(String tableName) {
        return "select * from " + tableName + " where ";
    }

    public CommonDataModel findRecords(Class<?> clazz, CommonDataModel element) {
        ArrayNode dynamicPrimaryKeys = fetchDynamicPrimaryKeys(clazz.getSimpleName());
        if (dynamicPrimaryKeys.size() > 0) {
            return findUniqueRecord(clazz, element, dynamicPrimaryKeys);
        } else {
            return findUniqueRecord(clazz, element);
        }

    }

    private Set<Field> findFields(Class<?> classs, Class<? extends Annotation> ann) {
        String key = classs.getName() + ":" + ann.getName();
        if (uniqueFieldsMap.containsKey(key)) {
            return uniqueFieldsMap.get(key);
        }
        Set<Field> set = new HashSet<>();
        Class<?> c = classs;
        while (c != null) {
            for (Field field : org.reflections.ReflectionUtils.getAllFields(c)) {
                if (field.isAnnotationPresent(ann)) {
                    set.add(field);
                }
            }
            c = c.getSuperclass();
        }
        uniqueFieldsMap.put(key, set);
        return set;
    }

    public String getTableField(Class<?> clazz, String entityField) {
        if (nativeFieldsMap.containsKey(clazz)) {
            Map<String, String> fields = nativeFieldsMap.get(clazz);
            if (fields.containsKey(entityField)) {
                return fields.get(entityField);
            }
        } else {
            nativeFieldsMap.put(clazz, new HashMap<>());
        }
        String fieldName = getFieldName(clazz, entityField);
        if (fieldName != null) {
            nativeFieldsMap.get(clazz).put(entityField, fieldName);
        }
        return fieldName;

    }

    private String getFieldName(Class<?> clazz, String entityField) {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        AbstractEntityPersister persister = ((AbstractEntityPersister) sessionFactory.getClassMetadata(clazz));
        for (Field field : org.reflections.ReflectionUtils.getAllFields(clazz)) {
            try {
                if (entityField.equals(field.getName())) {
                    String[] columnNames = persister.getPropertyColumnNames(field.getName());
                    if (columnNames.length > 0) {
                        return columnNames[0];
                    }
                }
            } catch (MappingException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug(ex.getMessage());
                }
            }
        }
        return null;
    }

    public List<UniqueKeyContainer> getUniqueKeyContainer(Class<?> clazz) {
        Set<Field> uniquefields = findFields(clazz, UniqueKey.class);
        List<UniqueKeyContainer> ukcontainer = new ArrayList<>();
        uniquefields.forEach(key -> {
            UniqueKeyContainer container = new UniqueKeyContainer();
            String tablefield = Optional.ofNullable(getTableField(clazz, key.getName()))
                    .orElse(Arrays.asList(key.getAnnotationsByType(UniqueKey.class)).get(0).nativeName());
            if (tablefield.equals("")) {
                throw new IllegalStateException("tablefield cannot be null");
            }
            container.setNativeName(tablefield);
            container.setField(key);
            String path = Arrays.asList(key.getAnnotationsByType(UniqueKey.class)).get(0).path();
            if (path.equals("")) path = key.getName();
            container.setPath(path);
            ukcontainer.add(container);
        });
        return ukcontainer;
    }

    public Object getFieldValue(Class<?> clazz, UniqueKeyContainer container, CommonDataModel cdm) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Object val = cdm;
        List<String> splitter = Arrays.asList(container.getPath().split("\\."));
        if (splitter.size() > 1) {
            try {
                val = PropertyUtils.getNestedProperty(val, container.getPath());
            } catch (NestedNullException nestex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Nested value found null for '{}'", container.getPath());
                }
                return null;
            } catch (InvocationTargetException | NoSuchMethodException e) {
                logger.error("Exception: ", e);
                return null;
            }
        } else {
            Field field = findField(clazz, container.getPath());
            field.setAccessible(true);
            val = field.get(val);
        }
        return val;
    }

    public Field findField(Class<?> clazz, String fieldName) {
        Class<?> c = clazz;
        Field tempfield = null;
        while (c != null) {
            for (Field field : org.reflections.ReflectionUtils.getAllFields(c)) {
                if (field.getName().equals(fieldName)) {
                    tempfield = field;
                    break;
                }
            }
            c = c.getSuperclass();
        }
        return tempfield;
    }

    public CommonDataModel findUniqueRecord(Class<?> clazz, CommonDataModel element) {
        try {
            String tablename = getTableName(clazz);
            List<UniqueKeyContainer> ukcontainer = getUniqueKeyContainer(clazz);
            if (ukcontainer.isEmpty()) {
                UniqueKeyContainer uk = new UniqueKeyContainer();
                uk.setNativeName("id");
                uk.setPath("id");
                ukcontainer.add(uk);
            }
            StringBuilder buffer1 = new StringBuilder(getBaseSQL(tablename));
            StringBuilder buffer2 = new StringBuilder();
            if (buffer2.length() > 0) {
                buffer2.append(" or ");
            }
            StringBuilder buffer3 = new StringBuilder();
            for (UniqueKeyContainer container : ukcontainer) {
                Object value = getFieldValue(clazz, container, element);
                fillTempbuffer(element, container, value, buffer3);
            }
            buffer2.append(buffer3);
            buffer1.append(buffer2);
            Query sqlquery = entitymanager.createNativeQuery(buffer1.toString(), clazz);
            return execute(sqlquery);
        } catch (NoResultException nex) {
            return null;
        } catch (Exception ex) {
            //throw new UnexpectedResultException(ex,"Error while fetching unique records. Reason : {}", ex.getMessage());
        }
        return null;
    }

//    public CommonDataModel findUniqueRecord(Class<?> clazz, CommonDataModel element) {
//        try {
//            String tablename = getTableName(clazz);
//            List<UniqueKeyContainer> ukcontainer = getUniqueKeyContainer(clazz);
//            if (ukcontainer.isEmpty()) {
//                UniqueKeyContainer uk = new UniqueKeyContainer();
//                uk.setNativeName("id");
//                uk.setPath("id");
//                ukcontainer.add(uk);
//            }
//            StringBuilder buffer1 = new StringBuilder(getBaseSQL(tablename));
//            StringBuilder buffer2 = new StringBuilder();
//            if (buffer2.length() > 0) {
//                buffer2.append(" or ");
//            }
//            StringBuilder buffer3 = new StringBuilder();
//            for (UniqueKeyContainer container : ukcontainer) {
//                Object value = getFieldValue(clazz, container, element);
//                fillTempbuffer(element,container,value,buffer3);
//            }
//            buffer2.append(buffer3);
//            buffer1.append(buffer2);
//            Query sqlquery = entitymanager.createNativeQuery(buffer1.toString(), clazz);
//            return execute(sqlquery);
//        }
//        catch(NoResultException nex) {
//            return null;
//        }
//        catch (Exception ex) {
//            throw new UnexpectedResultException(ex,"Error while fetching unique records. Reason : {}", ex.getMessage());
//        }
//
//    }

    private StringBuilder generateConditionFromValue(Object value) {
        StringBuilder result = new StringBuilder();
        if (value == null) {
            result.append(" is null ");
        } else if (String.valueOf(value).contains("'")) {
            result.append("\"").append(value).append("\"");
        } else {
            result.append("'").append(value).append("'");
        }
        return result;
    }

    private void fillTempbuffer(CommonDataModel element, UniqueKeyContainer container, Object value, StringBuilder buffer3)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        try {
            StringBuilder buffer4 = new StringBuilder();
            Class<?> propertyType = PropertyUtils.getPropertyType(element, container.getPath());
            if (propertyType == String.class || propertyType.isEnum()) {
                buffer4.append(generateConditionFromValue(value));
            } else {
                buffer4.append(value);
            }
            if (!buffer4.toString().isEmpty()) {
                if (buffer3.length() > 0) {
                    buffer3.append(" and ");
                }
                if (value != null) {
                    buffer3.append(container.getNativeName()).append("=").append(buffer4);
                } else {
                    buffer3.append(container.getNativeName()).append(buffer4);
                }
            }
        } catch (NestedNullException nestedex) {
            logger.debug("'{}' found null for unique key generation", container.getPath());
        }
    }

    private CommonDataModel execute(Query query) {
        try {
            return (CommonDataModel) query.getSingleResult();
        } catch (NoResultException e) {
            // we don't need to print this exception. If the query is not matching any record then this exception will throw.
            // The caller is expecting null as return type, so it's better to ignore this exception.
            return null;
        }
    }

    public ArrayNode fetchDynamicPrimaryKeys(String entityName) {
        MetaDataService metaDataSevice = SpringContext.getBean(MetaDataService.class);
        MetaData metaData = metaDataSevice.fetchByValue(entityName, "DynamicUniqueKey");
        ArrayNode columnArr = JSONUtils.getObjectMapper().createArrayNode();
        if (metaData != null) {
            columnArr = (ArrayNode) metaData.getDomainValues().get(0).get("dynamicKeys");
        }
        return columnArr;
    }

    public Field getClassField(Class<?> clazz, String tableField) {
        String key = clazz.getName() + ":" + tableField;
        if (tableFieldsMap.containsKey(key)) {
            return tableFieldsMap.get(key);
        }
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        AbstractEntityPersister persister = ((AbstractEntityPersister) sessionFactory.getClassMetadata(clazz));
        for (Field field : org.reflections.ReflectionUtils.getAllFields(clazz)) {
            try {
                String[] columnNames = persister.getPropertyColumnNames(field.getName());
                if (columnNames.length > 0) {
                    String column = columnNames[0];
                    if (tableField.equals(column)) {
                        tableFieldsMap.put(key, field);
                        return field;
                    }
                }
            } catch (MappingException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug(ex.getMessage());
                }
            }
        }

        return null;

    }

    public CommonDataModel findUniqueRecord(Class<?> clazz, CommonDataModel element, ArrayNode columnArr) {
        String tablename = getTableName(clazz);
        StringBuilder buffer1 = new StringBuilder(getBaseSQL(tablename));
        StringBuilder buffer2 = new StringBuilder();

        String value = "";
        for (int i = 0; i < columnArr.size(); i++) {
            String tempval = String.valueOf(getBeanProperty(element, columnArr.get(i).asText()));
            if (tempval != null) {
                if (value.isBlank()) {
                    value = tempval.toLowerCase();
                } else {
                    value = new StringBuilder(value).append("-").append(tempval.toLowerCase()).toString();
                }
                value = value.replace(" ", "-");
            }
        }
        buffer2.append("id").append("=").append("'").append(checkGenerateMD5Hash(clazz.getSimpleName()) ? StringUtils.escapeSql(EncodingUtils.getMd5(value)) : StringUtils.escapeSql(value)).append("'");
        buffer1.append(buffer2);
        Query sqlquery = entitymanager.createNativeQuery(buffer1.toString(), clazz);
        try {
            return (CommonDataModel) sqlquery.getSingleResult();
        } catch (NoResultException nr) {
            return null;
        }
    }

    public <T> String getTableName(Class<T> entityClass) {
        if (entityClass == null) {
            return null;
        }
        String fullqname = entityClass.getName();
        if (org.apache.commons.lang3.StringUtils.isBlank(fullqname)) {
            return null;
        }
        if (nativeTableNames.containsKey(fullqname)) {
            return nativeTableNames.get(fullqname);
        }
        Metamodel meta = entityManagerFactory.getMetamodel();
        EntityType<T> entityType = meta.entity(entityClass);
        Table t = entityClass.getAnnotation(Table.class);
        String tableName = (t == null)
                ? entityType.getName().toUpperCase()
                : t.name();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(tableName)) {
            nativeTableNames.put(fullqname, tableName);
        }
        return tableName;
    }

    private boolean checkGenerateMD5Hash(String entityName) {
        MetaDataService metaDataService = SpringContext.getBean(MetaDataService.class);
        MetaData metaData = metaDataService.fetchByValue(entityName, "DynamicUniqueKey");

        boolean generateHash = false;
        if (metaData != null && metaData.getDomainValues() != null) {
            JsonNode dynamicKeysNode = metaData.getDomainValues().get(0);
            if (dynamicKeysNode != null) {
                generateHash = dynamicKeysNode.has("generateHash") && dynamicKeysNode.get("generateHash").asBoolean();
            }
        }
        return generateHash;
    }

    public <T extends CommonDataModel> List<T> getGetKeyId(T cdmObject) {
        String cdmClassName = cdmObject.getClass().getSimpleName();
        MetaData metaConfig = metaDataService.fetchByValue(GET_KEY_QUERY_DOMAIN_NAME, cdmClassName);
        if (metaConfig != null && metaConfig.getDomainValues().get(0).has("query")) {
            String sql = metaConfig.getDomainValues().get(0).get("query").asText();
            Map<String, Object> params = mapper.convertValue(cdmObject, new TypeReference<>() {
            });
            String finalQuery = replaceDynamicKeys(sql, params);
            Query query = entitymanager.createQuery(finalQuery);
            return query.getResultList();

        } else {
            return new ArrayList<>(1);
        }
    }

    public String generateId(CommonDataModel cdm, boolean findByDynamicKeyOnly) {
        String genratedId = "";
        String entityName = cdm.getClass().getSimpleName();
        ArrayNode columnArr = fetchDynamicPrimaryKeys(entityName);
        boolean generateHash = checkGenerateMD5Hash(entityName);
        if (columnArr.size() != 0) {
            for (int i = 0; i < columnArr.size(); i++) {
                String columnName = columnArr.get(i).asText();
                String value = String.valueOf(getBeanProperty(cdm, columnName));
                if (value != null) {
                    if (genratedId.isBlank()) {
                        genratedId = value.toLowerCase();
                    } else {
                        genratedId = new StringBuffer(genratedId).append("-").append(value.toLowerCase()).toString();
                    }
                    genratedId = genratedId.replace(" ", "-");
                }
            }
        } else if (findByDynamicKeyOnly) {
            //  throw new ValidationFailedException("dynamic key doesnot exist");
        } else {
            genratedId = UUID.randomUUID().toString();
        }
        if (generateHash) {
            return genratedId.equals("") ? UUID.randomUUID().toString() : EncodingUtils.getMd5(genratedId);
        }
        return genratedId.equals("") ? UUID.randomUUID().toString() : genratedId;
    }

    public <T> List<?> findDataByQuery(Class<T> clazz, String query, boolean isNative) {
        if (NullUtils.isNotNull(query)) {
            List<T> data = null;
            if (isNative) {
                Query q = null;
                if (clazz == List.class) {
                    q = entitymanager.createNativeQuery(query);
                } else if (clazz == Map.class) {
                    q = entitymanager.createNativeQuery(query);
                    q.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
                } else {
                    q = entitymanager.createNativeQuery(query, clazz);
                }
                q.setHint(QueryHints.NATIVE_LOCKMODE, LockModeType.NONE);
                data = q.getResultList();
            } else {
                TypedQuery<T> q = entitymanager.createQuery(query, clazz);
                data = q.getResultList();
            }
            return data;
        }
        return List.of();
    }

    @Override
    public void afterPropertiesSet() {
        setInstance(this);
    }

    public EntityInfo getEntityInfo(String entityName) {
        return getEntityInfo(getEntityClass(entityName));
    }

    /**
     * Gets the table field.
     *
     * @param clazz the clazz
     * @return the field
     */
    public EntityInfo getEntityInfo(Class<?> clazz) {
        if (entityInfoMap.containsKey(clazz.getName())) {
            return entityInfoMap.get(clazz.getName());
        }
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        AbstractEntityPersister persister = ((AbstractEntityPersister) sessionFactory.getClassMetadata(clazz));
        EntityInfo einfo = new EntityInfo();
        einfo.setClassName(clazz.getName());
        einfo.setTableName(getTableName(clazz));
        Map<String, String> fieldMap = new LinkedHashMap<>();
        Map<String, EntityFieldInfo> fieldInfoMap = new LinkedHashMap<>();

        for (Field field : org.reflections.ReflectionUtils.getAllFields(clazz)) {
            try {
                String[] columnNames = persister.getPropertyColumnNames(field.getName());
                if (columnNames.length > 0) {
                    String column = columnNames[0];
                    fieldMap.put(field.getName(), column);
                    fieldInfoMap.put(field.getName(),getFieldInfo(field,column));
                }
            } catch (MappingException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug(ex.getMessage());
                }
            }
        }
        einfo.setFieldNameMap(fieldMap);
        einfo.setFieldInfoMap(fieldInfoMap);
        entityInfoMap.put(clazz.getName(), einfo);
        return einfo;
    }

    private EntityFieldInfo getFieldInfo(Field field,String columnName){
        var finfo = new EntityFieldInfo();
        finfo.setName(columnName);
        finfo.setDataType(field.getType().getSimpleName());
        finfo.setTypeClass(field.getType().getName());
        finfo.setPrimitive(field.getType().isPrimitive());
        return finfo;
    }
}