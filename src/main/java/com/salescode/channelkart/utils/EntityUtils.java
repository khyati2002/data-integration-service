package com.salescode.channelkart.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.models.CommonDataModel;

import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.templates.TemplateEngine;

import com.salescode.dataintegration.etl.metadata.registry.MetadataRegistry;
import com.salescode.jooq.generated.Tables;
import com.salescode.jooq.generated.tables.pojos.CkMetadata;
import lombok.SneakyThrows;
import org.apache.commons.beanutils.BeanUtils;
import org.jooq.DSLContext;
import org.jooq.impl.TableImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public final class EntityUtils {

    private static final Logger logger = LoggerFactory.getLogger(EntityUtils.class);
    private static final String GET_KEY_QUERY_DOMAIN_NAME = "cdmGetKeyQuery";
    private static final List<Class> jsonNodeClassList = new ArrayList<>(Arrays.asList(JsonNode.class, ObjectNode.class));
    private static final Map<String, Class<? extends CommonDataModel>> entityClassMap = new ConcurrentHashMap<>();
    private static final Map<String, String> nativeTableNames = new HashMap<>();
    private static EntityUtils instance;
    private final MetadataRegistry metadataRegistry;
    private final DSLContext dslContext;

    private static final Object lockObj = new Object();


    @Autowired
    public EntityUtils(MetadataRegistry metadataRegistry, DSLContext dslContext) {
        this.metadataRegistry = metadataRegistry;
        this.dslContext = dslContext;
        instance = this;
    }

    public static EntityUtils getInstance() {
        if (instance == null) {
            throw new IllegalStateException("EntityUtils is not initialized yet");
        }
        return instance;
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
            throw new RuntimeException("Could not clone object:" + src);
        }
    }

    @SneakyThrows
    public <T extends CommonDataModel> TableImpl getDSLContextTable(Class<T> entityClass) {
        Field ckOutletDetails = Arrays.stream(Tables.class.getFields()).filter(s -> s.getType().getSimpleName().equals(entityClass.getSimpleName())).findAny().orElseThrow();
        ckOutletDetails.setAccessible(true);
        TableImpl table = (TableImpl) ckOutletDetails.get(null);
        return table;
    }

    public String replaceDynamicKeys(String string, Map<String, Object> params) {
        TemplateEngine templateEngine = SpringContext.getBean(TemplateEngine.class);
        return templateEngine.applyInline(string, params);
    }

    public Class<? extends CommonDataModel> getEntityClass(String entityName) {
        if (entityClassMap.containsKey(entityName)) {
            return entityClassMap.get(entityName);
        }
        Set<Class<? extends CommonDataModel>> subClasses = ReflectionUtils.findSubClasses(CommonDataModel.class);
        for (var entity : subClasses) {
            if (entityName.equalsIgnoreCase(entity.getSimpleName())) {
                entityClassMap.put(entityName, entity);
                return entity;
            }
        }
        throw new IllegalArgumentException("Entity not found: " + entityName);
    }

    public <T extends CommonDataModel> List<T> getGetKeyId(T cdmObject) {
        Class<T> clazz = (Class<T>) cdmObject.getClass();
        String cdmClassName = clazz.getSimpleName();
        CkMetadata metaConfig = metadataRegistry.getMetadataByDomainNameAndType(GET_KEY_QUERY_DOMAIN_NAME, cdmClassName).orElse(null);
        if (metaConfig != null && metaConfig.getDomainValues().get(0).has("query")) {
            String sql = metaConfig.getDomainValues().get(0).get("query").asText();
            Map<String, Object> params = JSONUtils.getObjectMapper().convertValue(cdmObject, new TypeReference<>() {});
            String finalQuery = replaceDynamicKeys(sql, params);
            return dslContext.selectFrom(finalQuery).fetchInto(clazz);
        } else {
            return new ArrayList<>(1);
        }
    }

    public String generateId(CommonDataModel cdm, boolean findByDynamicKeyOnly) {
        String genratedId = "";
        String entityName = cdm.getClass().getSimpleName();
        ArrayNode columnArr = fetchDynamicPrimaryKeys(entityName);
        boolean generateHash = checkGenerateMD5Hash(entityName);
        if (!columnArr.isEmpty()) {
            for (int i = 0; i < columnArr.size(); i++) {
                String columnName = columnArr.get(i).asText();
                String value = String.valueOf(getBeanProperty(cdm, columnName));
                if (value != null) {
                    if (genratedId.isBlank()) {
                        genratedId = value.toLowerCase();
                    } else {
                        genratedId = genratedId + "-" + value.toLowerCase();
                    }
                    genratedId = genratedId.replace(" ", "-");
                }
            }
        } else if (findByDynamicKeyOnly) {
            throw new RuntimeException("dynamic key does not exist");
        } else {
            genratedId = UUID.randomUUID().toString();
        }
        if (generateHash) {
            return genratedId.isEmpty() ? UUID.randomUUID().toString() : EncodingUtils.getMd5(genratedId);
        }
        return genratedId.isEmpty() ? UUID.randomUUID().toString() : genratedId;
    }

    public ArrayNode fetchDynamicPrimaryKeys(String entityName) {
        CkMetadata metaData = metadataRegistry.getMetadataByDomainNameAndType(entityName, "DynamicUniqueKey").orElse(null);
        ArrayNode columnArr = JSONUtils.getObjectMapper().createArrayNode();
        if (metaData != null) {
            columnArr = (ArrayNode) metaData.getDomainValues().get(0).get("dynamicKeys");
        }
        return columnArr;
    }

    private boolean checkGenerateMD5Hash(String entityName) {
        CkMetadata metaData = metadataRegistry.getMetadataByDomainNameAndType(entityName, "DynamicUniqueKey").orElse(null);
        boolean generateHash = false;
        if (metaData != null && metaData.getDomainValues() != null) {
            JsonNode dynamicKeysNode = metaData.getDomainValues().get(0);
            if (dynamicKeysNode != null) {
                generateHash = dynamicKeysNode.has("generateHash") && dynamicKeysNode.get("generateHash").asBoolean();
            }
        }
        return generateHash;
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
                } else if (field.length < 2)
                    throw new CustomRuntimeException("Key is not defied for json node {}'", property);
                else
                    throw new CustomRuntimeException("Nested Json Key [{}] is not supported in dynamic key creation.", property);
            }
            return BeanUtils.getProperty(cdm, property);
        } catch (Exception e) {
            logger.debug("could not find property {} from cdm object {}, class {}", property, cdm, cdm.getClass());
            logger.error(e.getMessage());
            if (e instanceof CustomRuntimeException) throw new CustomRuntimeException(e);
            return "";
        }
    }

    @SneakyThrows
    public CommonDataModel findRecords(Class<? extends CommonDataModel> clazz, CommonDataModel element ) {
        ArrayNode dynamicPrimaryKeys = fetchDynamicPrimaryKeys(clazz.getSimpleName());
        if (!dynamicPrimaryKeys.isEmpty()) {
            return findUniqueRecord(clazz, element, dynamicPrimaryKeys,new HashMap<>());
        } else {
         //  return findUniqueRecord(clazz, element);
        }
        return null;
    }

    @SneakyThrows
    public CommonDataModel findRecords(Class<? extends CommonDataModel> clazz, CommonDataModel element,Map<String,? extends CommonDataModel> recordsMap) {
        ArrayNode dynamicPrimaryKeys = fetchDynamicPrimaryKeys(clazz.getSimpleName());
        if (!dynamicPrimaryKeys.isEmpty()) {
            return findUniqueRecord(clazz, element, dynamicPrimaryKeys, recordsMap);
        } else {
            //  return findUniqueRecord(clazz, element);
        }
        return null;
    }

//    public CommonDataModel findUniqueRecord(Class<? extends CommonDataModel> clazz, CommonDataModel element) {
//        String tablename = getTableName(clazz);
//       TableImpl dslContextTable = getDSLContextTable(clazz);
////        List<? extends TableField<?, ?>> list = dslContext.meta(dslContextTable).getUniqueKeys().stream().flatMap(s -> s.getFields().stream()).toList();
//        dslContext.meta(dslContextTable).getUniqueKeys()
//                .stream()
//                .flatMap(s -> s.getFields().stream())
//                .flatMap(s -> Arrays.stream(s.getUnqualifiedName().unquotedName().getName()))
//                .toList();
//        return null;
//    }

    @SneakyThrows
    public <T extends CommonDataModel> String getTableName(Class<T> entityClass) {
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
        TableImpl table = getDSLContextTable(entityClass);
        String tableName = table.getName();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(tableName)) {
            nativeTableNames.put(fullqname, tableName);
        }
        return tableName;
    }

    private String getBaseSQL(String tableName) {
        return "select * from " + tableName + " where ";
    }

    public CommonDataModel findUniqueRecord(Class<? extends CommonDataModel> clazz, CommonDataModel element, ArrayNode columnArr,Map<String,? extends CommonDataModel> recordsMap) {
        StringBuilder buffer2 = new StringBuilder();

        String value = "";
        String mapValue = "";
        for (int i = 0; i < columnArr.size(); i++) {
            String tempval = String.valueOf(getBeanProperty(element, columnArr.get(i).asText()));
            if (tempval != null) {
                value = tempval;
                String colName = columnArr.get(i).toString();
                if(buffer2.length()> 0) {
                    mapValue += tempval;
                    buffer2.append(" and ").append(columnArr.get(i).toString().substring(1, colName.length() - 1)).append("=").append("'").append(StringUtils.escapeSql(value)).append("'");
                }
                else{
                    mapValue += "-" + tempval;
                    buffer2.append(columnArr.get(i).toString().substring(1, colName.length() - 1)).append("=").append("'").append(StringUtils.escapeSql(value)).append("'");
                }
            }
        }

        if(recordsMap.containsKey(mapValue)){
            return recordsMap.get(mapValue);
        }
//        Query sqlquery = entitymanager.createNativeQuery(buffer1.toString(), clazz);
        try {
            return (CommonDataModel)  Objects.requireNonNull(dslContext.selectFrom(getDSLContextTable(clazz)).where(buffer2.toString())).fetchAnyInto(clazz);
        } catch (NoResultException nr) {
            //throw new RuntimeException("Invalid ");
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
                Object propertyValue= source.getPropertyValue(pd.getName());
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
            if(strings!=null) {
                fields.addAll(
                        Arrays.asList(strings).stream().filter(Objects::nonNull).collect(Collectors.toList()));
            }
            BeanWrapper source = new BeanWrapperImpl(src);
            BeanWrapper target = new BeanWrapperImpl(tgt);
            java.beans.PropertyDescriptor[] pdsrc = source.getPropertyDescriptors();
            for (java.beans.PropertyDescriptor pd : pdsrc) {
                Object propertyValue= source.getPropertyValue(pd.getName());
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


}