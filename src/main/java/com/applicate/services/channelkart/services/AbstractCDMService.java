package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.jooq.impl.OutletDetails;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractCDMService<T extends CommonDataModel> implements CommonDataModelService<T> {

    @Getter
    private Class<T> persistentClass;

    @Getter
    private static DSLContext dslContext;

    public AbstractCDMService() {

        if (getClass().getGenericSuperclass() instanceof ParameterizedType) {
            Type genericSuperclass = getClass().getGenericSuperclass();
            ParameterizedType paramType = (ParameterizedType) genericSuperclass;
            this.persistentClass = (Class<T>) paramType.getActualTypeArguments()[0];
        }
    }

    public static void setDslContext(DSLContext dslContext) {
        if (AbstractCDMService.dslContext == null) {
            AbstractCDMService.dslContext = dslContext;
        }
    }

    @Override
    public Collection<T> batchSave(Collection<T> cdmObject) {
        return cdmObject.stream().map(this::save).collect(Collectors.toList());
    }

    public <T extends CommonDataModel> T addHash(T model) {
        String hash = model.hash();
        model.setHash(hash);
        return model;
    }

    @Override
    public T save(T cdmObject) {
        return batchSave(List.of(cdmObject)).stream().findFirst().get();
    }

    public static <T> void fillAttributes(T target, T source) {
        if (target == null || source == null) return;

        Field[] fields = getImmediateFields(target.getClass()); // Get all fields (including superclass)

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object targetValue = field.get(target);
                Object sourceValue = field.get(source);

                if (targetValue == null && sourceValue != null) {
                    field.set(target, sourceValue); // Copy value if target is null
                } else if (field.getName().equals("extendedAttributes") && targetValue instanceof JsonNode && sourceValue instanceof JsonNode) {
                    // Merge JSON fields if both are JsonNode
                    JsonNode mergedJson = mergeJson((JsonNode) sourceValue, (JsonNode) targetValue);
                    field.set(target, mergedJson);
                }

            } catch (IllegalAccessException e) {
                e.printStackTrace(); // Handle exception properly
            }
        }
    }



    public static Field[] getImmediateFields(Class<?> targetClass) {
        boolean isImplClass = targetClass.getName().contains(".impl");

        // Always include fields from the current class
        Stream<Field> fieldsStream = Arrays.stream(targetClass.getDeclaredFields());

        // If the class is ".impl", also include fields from its direct superclass (excluding CommonDataModel)
        Class<?> superClass = targetClass.getSuperclass();
        if (isImplClass && superClass != null && superClass != CommonDataModel.class) {
            fieldsStream = Stream.concat(fieldsStream, Arrays.stream(superClass.getDeclaredFields()));
        }

        return fieldsStream.toArray(Field[]::new);
    }



    public void fillCommonAttributes(T cdmObject){

        if(cdmObject.getCreationTime() == null){
            cdmObject.setCreationTime(LocalDateTime.now(ZoneOffset.UTC));
        }

        cdmObject.setLastModifiedTime(LocalDateTime.now(ZoneOffset.UTC));

        if (cdmObject.getCreatedBy() == null) {
            cdmObject.setCreatedBy(SecurityContextUtils.getPrincipal());
        }
        if (cdmObject.getModifiedBy() == null) {
            cdmObject.setModifiedBy(SecurityContextUtils.getPrincipal());
        }

        if(cdmObject.getLob() == null){
            cdmObject.setLob(SecurityContextUtils.getLob());
        }
    }

    private static JsonNode mergeJson(JsonNode base, JsonNode updates) {
        if (base == null) return updates;
        if (updates == null) return base;

        ObjectNode mergedNode = base.deepCopy();
        updates.fields().forEachRemaining(entry -> mergedNode.set(entry.getKey(), entry.getValue()));
        return mergedNode;
    }


}
