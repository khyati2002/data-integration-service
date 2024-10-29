package com.salescode.channelkart.utils;

import com.salescode.channelkart.models.CommonDataModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EntityUtils {

    private static final Map<String, Class<? extends CommonDataModel>> entityClassMap = new ConcurrentHashMap<>();

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

    public static Class<? extends CommonDataModel> getEntityClass(String entityName) {
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
}