package com.applicate.services.channelkart.utils;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.utils.ReflectionUtils;
import org.jooq.DSLContext;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EntityUtils {

    private static volatile EntityUtils instance;
    private final transient DSLContext dslContext;
    private final Map<String, Class<? extends CommonDataModel>> entityImplClassMap = new ConcurrentHashMap<>();
    Set<Class<? extends CommonDataModel>> subClasses = ReflectionUtils.findSubClasses(CommonDataModel.class);

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
}