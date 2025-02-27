package com.applicate.services.channelkart.utils;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.jooq.generated.Tables;
import com.salescode.dim.utils.ReflectionUtils;
import lombok.SneakyThrows;
import org.jooq.DSLContext;
import org.jooq.impl.TableImpl;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntityUtils {

    private static final Map<String, Class<? extends CommonDataModel>> entityClassMap = new ConcurrentHashMap<>();
    private static volatile EntityUtils instance;
    private final transient DSLContext dslContext;
    private final Map<String, Class<? extends CommonDataModel>> entityImplClassMap = new HashMap<>();
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

    public Set<String> getUniqueKeys(Class<? extends CommonDataModel> aClass) {
        Set<String> hs = new HashSet<>();
        hs.add("loginid");
        return hs;
    }

    public Class<? extends CommonDataModel> getEntityClass(String entityName) {
        return entityImplClassMap.computeIfAbsent(entityName, key -> {
            List<Class<? extends CommonDataModel>> candidates = subClasses.stream()
                                                                          .filter(e -> key.equalsIgnoreCase(e.getSimpleName()))
                                                                          .collect(Collectors.toList());
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("Entity not found: " + key);
            }
            return candidates.stream()
                             .filter(e -> e.getPackage().getName().contains(".impl"))
                             .findFirst()
                             .orElse(candidates.get(0));
        });
    }

    @SneakyThrows
    public <T extends CommonDataModel> TableImpl getDSLContextTable(Class<T> entityClass) {
        String str = entityClass.getSimpleName();
        String tables = Tables.class.getSimpleName();
        Field[] fields = Tables.class.getDeclaredFields();
        List<String> tableNames = Arrays.stream(Tables.class.getFields())
                .map(f -> f.getName() + " -> " + f.getType().getSimpleName())
                .collect(Collectors.toList());

// Store or log the names for debugging
        System.out.println("Available tables: " + tableNames);

        Optional<Field> optionalField = Arrays.stream(Tables.class.getFields())
                .filter(s -> s.getType().getSimpleName().equals("Ck" + entityClass.getSimpleName()))
                .findAny();

        if (optionalField.isEmpty()) {
            throw new IllegalStateException("No matching table found for entity: " + entityClass.getSimpleName());
        }
        Field ckOutletDetails = optionalField.get();
        ckOutletDetails.setAccessible(true);
         TableImpl table = (TableImpl) ckOutletDetails.get(null);
        return table;
    }

}