package com.applicate.services.channelkart.utils;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.services.SalesService;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.services.SalesService;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import com.salescode.dim.utils.ReflectionUtils;
import jakarta.activation.DataHandler;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class EntityUtils {

    private static volatile EntityUtils instance;
    private final transient DSLContext dslContext;
    private final Map<String, Class<? extends CommonDataModel>> entityImplClassMap = new ConcurrentHashMap<>();
    public static final String DYNAMIC_UNIQUE_KEY = "DynamicUniqueKey";
    private static final MetaDataService metadataService=new MetaDataService();
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(EntityUtils.class);


    Set<Class<? extends CommonDataModel>> subClasses = ReflectionUtils.findSubClasses(CommonDataModel.class);
    private static final Object lockObj = new Object();
    private static final Set<Class<?>> jsonNodeClassList = Collections.singleton(JsonNode.class);
    private static class Logger { void error(String msg, Exception e) { System.err.println(msg); e.printStackTrace(); } }

    public EntityUtils(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public static EntityUtils getInstance(DSLContext dslContext) {
        if (instance == null) {
            synchronized (EntityUtils.class) {
                if (instance == null) {
                    instance = new EntityUtils(dslContext);
                }
            }
        }
        return instance;
    }

    public static EntityUtils getInstance() {
        if (instance == null) {
            throw new IllegalStateException("EntityUtils was not initialized");
        }
        return instance;
    }


    public Class<? extends CommonDataModel> getEntityClass(String entityName) {
        return entityImplClassMap.computeIfAbsent(entityName, key -> {
            List<Class<? extends CommonDataModel>> candidates = subClasses.stream()
                    .filter(e -> key.equalsIgnoreCase(e.getSimpleName())).collect(Collectors.toList());
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("Entity not found: " + key);
            }
            return candidates.stream().filter(e -> e.getPackage().getName().contains(".impl")).findFirst()
                    .orElse(candidates.get(0));
        });
    }

    public static String getMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashtext = new StringBuilder(no.toString(16));
            while (hashtext.length() < 32) {
                hashtext.append( "0" + hashtext);
            }
            return hashtext.toString();
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkGenerateMD5Hash(String entityName) {
        Metadata metaData = metadataService.fetchByValue(entityName, DYNAMIC_UNIQUE_KEY);

        boolean generateHash = false;
        if (metaData != null && metaData.getDomainValues()!=null) {
            JsonNode dynamicKeysNode = metaData.getDomainValues().get(0);
            if (dynamicKeysNode != null) {
                generateHash = dynamicKeysNode.has("generateHash") && dynamicKeysNode.get("generateHash").asBoolean();
            }
        }
        return generateHash;
    }

    public ArrayNode fetchDynamicPrimaryKeys(String entityName) {
        Metadata metaData=  metadataService.fetchByValue(entityName,DYNAMIC_UNIQUE_KEY);
        org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode columnArr = JSONUtils.getObjectMapper().createArrayNode();
        if(metaData!=null && metaData.getDomainValues()!=null) {
            var dynamicKeys=metaData.getDomainValues().get(0).get("dynamicKeys");
            if(dynamicKeys!=null) {
                columnArr = JSONUtils.convertToArrayNode(dynamicKeys);
            }
        }
        return columnArr;
    }


    public static void copyPropertiesWithoutMerging(Object src, Object tgt, String... strings) {
        copyPropertiesWithJsonNodeHandling(src, tgt, false, strings);
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

    public <T> List<?> findDataByQuery(Class<T> clazz, String query, boolean isNative) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        // Run raw SQL using JOOQ
        Result<Record> result = dslContext.fetch(query);

        if (clazz == Map.class) {
            // Convert to list of maps (column alias -> value)
            return result.stream()
                    .map(Record::intoMap)
                    .collect(Collectors.toList());
        } else if (clazz == List.class || clazz == Record.class) {
            // Return raw records
            return result;
        } else {
            // Convert into the provided POJO class
            return result.into(clazz);
        }
    }

    public static String[] getNullPropertyNames(Object source) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(source.getClass());
            PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();

            Set<String> emptyNames = new HashSet<>();
            for (PropertyDescriptor pd : pds) {
                Method getter = pd.getReadMethod();
                if (getter != null) {
                    Object srcValue = getter.invoke(source);
                    if (srcValue == null || isNullNode(srcValue)) {
                        emptyNames.add(pd.getName());
                    }
                }
            }
            return emptyNames.toArray(new String[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isNullNode(Object value) {
        return value instanceof JsonNode && ((JsonNode) value).isNull();
    }

    public static void copyPropertiesWithJsonNodeHandling(Object src, Object tgt, boolean mergeJsonNodes, String... strings) {
        synchronized (lockObj) {
            String[] data = getNullPropertyNames(src);
            Set<String> fields = new HashSet<>(Arrays.asList(data));

            if (strings != null) {
                fields.addAll(Arrays.stream(strings)
                                      .filter(Objects::nonNull)
                                      .collect(Collectors.toList()));
            }

            BeanInfo beanInfo;
            try {
                beanInfo = Introspector.getBeanInfo(src.getClass());
            } catch (IntrospectionException e) {
                throw new RuntimeException(e);
            }
            PropertyDescriptor[] pdsrc = beanInfo.getPropertyDescriptors();

            for (PropertyDescriptor pd : pdsrc) {
                try {
                    Method getter = pd.getReadMethod();
                    Method setter = pd.getWriteMethod();
                    if (getter == null || setter == null) continue;
                    Object propertyValue = getter.invoke(src);
                    if (propertyValue != null
                                && JsonNode.class.isAssignableFrom(pd.getPropertyType())
                                && !fields.contains(pd.getName())
                                && jsonNodeClassList.contains(propertyValue.getClass())) {

                        fields.add(pd.getName());
                        Object tgtValue = getter.invoke(tgt);
                        if (tgtValue == null || isNullNode(tgtValue)) {
                            setter.invoke(tgt, propertyValue);
                        } else if (mergeJsonNodes) {
                            JsonNode mergedJson = null;
                            try {
                                mergedJson = JSONUtils.mergeJsonNodes((JsonNode) propertyValue, (JsonNode) tgtValue);
                            } catch (java.io.IOException e) {
                                LOG.error("stacktrace", e);
                            }
                            setter.invoke(tgt, mergedJson);
                        } else {
                            setter.invoke(tgt, propertyValue);
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Property copy error", e);
                }
            }
            for (PropertyDescriptor pd : pdsrc) {
                if (!fields.contains(pd.getName())) {
                    try {
                        Method getter = pd.getReadMethod();
                        Method setter = pd.getWriteMethod();
                        if (getter != null && setter != null) {
                            Object value = getter.invoke(src);
                            setter.invoke(tgt, value);
                        }
                    } catch (Exception e) {
                        LOG.error("Property copy error", e);
                    }
                }
            }
        }
    }


}