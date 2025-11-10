package com.applicate.services.channelkart.utils;

import com.applicate.services.channelkart.models.CommonDataModel;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dim.utils.ReflectionUtils;
import org.jooq.DSLContext;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EntityUtils {

    private static volatile EntityUtils instance;
    private final transient DSLContext dslContext;
    private final Map<String, Class<? extends CommonDataModel>> entityImplClassMap = new ConcurrentHashMap<>();
    Set<Class<? extends CommonDataModel>> subClasses = ReflectionUtils.findSubClasses(CommonDataModel.class);
    private static final Object lockObj = new Object();
    private static final Set<Class<?>> jsonNodeClassList = Collections.singleton(JsonNode.class);
    private static class Logger { void error(String msg, Exception e) { System.err.println(msg); e.printStackTrace(); } }
    private static final Logger logger = new Logger();

    protected EntityUtils(DSLContext dslContext) {
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
                                logger.error("stacktrace", e);
                            }
                            setter.invoke(tgt, mergedJson);
                        } else {
                            setter.invoke(tgt, propertyValue);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Property copy error", e);
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
                        logger.error("Property copy error", e);
                    }
                }
            }
        }
    }


}