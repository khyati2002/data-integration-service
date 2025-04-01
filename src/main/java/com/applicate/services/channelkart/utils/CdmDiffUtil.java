package com.applicate.services.channelkart.utils;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.diff.Change;
import com.applicate.services.channelkart.models.diff.ChangeType;
import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dim.utils.ReflectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * created by Siddarth Sreeni on 07-05-2021
 * <p>
 * This class returns a Set of {@link Change} .class from any Object T to previous Object T.
 * Although this method currently only compares between two CommonFataModel,
 * It can be used to compare any two generic objects
 *
 * @see Change
 */
public class CdmDiffUtil {

    private static final Map<String, Set<Field>> CACHED_FIELDS = new ConcurrentHashMap<>();

    private CdmDiffUtil() {
        throw new UnsupportedOperationException();
    }

    private static final Logger log = LoggerFactory.getLogger(CdmDiffUtil.class);

    /**
     * Returns
     *
     * @param current  instance of the Object which is to be compared.
     * @param previous instance of the same Object which is to be compared with.
     * @return a Set<Change<>> objects.
     */
    @SuppressWarnings("java:S3011")
    public static <T extends CommonDataModel> Set<Change<Serializable>> getChanges(T current, T previous) {
        if (!current.getClass().equals(previous.getClass())) {
            throw new IllegalArgumentException("both current & previous have different class types!");
        }

        Set<Change<Serializable>> changes = new HashSet<>(10);

        for (Field field : getInheritedPrivateFields(current.getClass())) {
            try {
                field.setAccessible(true);
                Serializable currentField = (Serializable) field.get(current);
                Serializable previousField = (Serializable) field.get(previous);

                        if (currentField instanceof CommonDataModel) {
                            continue; // Skip if currentField is an instance of CommonDataModel
                        }

                        if (currentField instanceof List<?>) {
                            List<?> list = (List<?>) currentField; // Manually cast

                            if (!list.isEmpty() && list.get(0) instanceof CommonDataModel) {
                                continue; // Skip if the list contains instances of CommonDataModel
                            }
                        }
                    Change<Serializable> change = new Change<>(field.getName(), currentField, previousField);
                    if (!change.getChangeType().equals(ChangeType.NOCHANGE)) {
                        changes.add(change);
                    }

            } catch (IllegalAccessException e) {
                log.error("Retrieving field for class type {}, throw error {}", current.getClass().getSimpleName(), ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return changes;
    }

    /**
     * Currently the changes are evaluated across the following valid type.
     *
     * @param retType the type of the class.
     * @return if retType is a primitive or boxed primitive class.
     */
    private static boolean isValidType(Class<?> retType) {
        if (retType.isPrimitive() && retType != void.class) return true;
        if (Number.class.isAssignableFrom(retType)) return true;
        if (Boolean.class == retType) return true;
        if (Character.class == retType) return true;
        if (String.class == retType) return true;
        if (Date.class.isAssignableFrom(retType)) return true;
        if (byte[].class.isAssignableFrom(retType)) return true;
        if (Collection.class.isAssignableFrom(retType)) return true;
        if(JsonNode.class.isAssignableFrom(retType)) return true;
        if(Map.class.isAssignableFrom(retType)) return true;
        if(CommonDataModel.class.isAssignableFrom(retType)) return true;
        return Enum.class.isAssignableFrom(retType);
    }

    /**
     * Currently the chagnes are evaluated also for the following List<Primitive> & Set<Primitive>
     *
     * @param field method of the Object.
     * @return true if this is of List<`a valid primitive type`>
     */
    private static boolean isValidPrimitiveParameter(Field field) {
        if (isAssignableFromCollection(field) && (field.getGenericType() instanceof ParameterizedType)) {
            ParameterizedType primitiveType = (ParameterizedType) field.getGenericType();
            if (primitiveType.getActualTypeArguments() != null
                    && primitiveType.getActualTypeArguments().length > 0
                    && primitiveType.getActualTypeArguments()[0] instanceof Class<?>) {
                return isValidType((Class<?>) primitiveType.getActualTypeArguments()[0]);
            }
        }
        return false;
    }

    private static boolean isAssignableFromCollection(Field field) {
        return List.class.isAssignableFrom(field.getType()) || Set.class.isAssignableFrom(field.getType());
    }

    /**
     * A recursive approach to find all private fields of all base classes of this child class.
     *
     * @param type of the base class
     * @return all the private inherited fields of the object & it's base class.
     */
    private static Set<Field> getInheritedPrivateFields(Class<? extends CommonDataModel> type) {
        String name = type.getSimpleName();
        if (CACHED_FIELDS.containsKey(name)) {
            return CACHED_FIELDS.get(name);
        }
        synchronized (CACHED_FIELDS) {
            if (CACHED_FIELDS.containsKey(name)) {
                return CACHED_FIELDS.get(name);
            }
            Set<Field> fields = ReflectionUtils.getInstanceFields(type, Set.of("changed","lastModifiedTime","hash","version"));
            CACHED_FIELDS.put(name, fields);
            return fields;
        }
    }

}
