package com.applicate.services.channelkart.utils;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.utils.ReflectionUtils;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

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


}