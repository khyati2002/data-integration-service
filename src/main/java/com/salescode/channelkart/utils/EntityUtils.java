package com.salescode.channelkart.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.services.CommonDataModelService;
import com.salescode.channelkart.services.ServiceLocator;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.templates.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.NoResultException;
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
    private static final Object lockObj = new Object();
    private static EntityUtils instance;

    Set<Class<? extends CommonDataModel>> subClasses = ReflectionUtils.findSubClasses(CommonDataModel.class);


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
                } else if (field.length < 2)
                    throw new CustomRuntimeException("Key is not defied for json node {}'", property);
                else
                    throw new CustomRuntimeException("Nested Json Key [{}] is not supported in dynamic key creation.", property);
            }
         //   return BeanUtils.getProperty(cdm, property);
        } catch (Exception e) {
            logger.debug("could not find property {} from cdm object {}, class {}", property, cdm, cdm.getClass());
            logger.error(e.getMessage());
            if (e instanceof CustomRuntimeException) throw new CustomRuntimeException(e);
            return "";
        }
        return null;
    }


    private String getBaseSQL(String tableName) {
        return "select * from " + tableName + " where ";
    }





}