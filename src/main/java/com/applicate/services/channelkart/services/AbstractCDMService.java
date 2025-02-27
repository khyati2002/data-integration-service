package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.jooq.impl.Location;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.C;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractCDMService<T extends CommonDataModel> implements CommonDataModelService<T>{

    private Class<T> persistentClass;

    private DSLContext dsl;

    public AbstractCDMService(DSLContext dsl) {

        if (getClass().getGenericSuperclass() instanceof ParameterizedType) {
            persistentClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            ServiceLocator.register(persistentClass, this);
        }
        this.dsl = dsl;
    }

    @SneakyThrows
    public List<T> getExisting(List<T> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        // Get the class of the items
        Class<? extends CommonDataModel> clazz = items.get(0).getClass();

        // Use EntityUtils to get unique keys
        Set<String> uniqueKeys = EntityUtils.getInstance().getUniqueKeys(clazz);
        if (uniqueKeys == null || uniqueKeys.isEmpty()) {
            throw new IllegalArgumentException("No unique key fields found for class " + clazz.getName());
        }

        // Get the first unique key field
        String uniqueKeyName = uniqueKeys.stream().findFirst().get();
        java.lang.reflect.Field uniqueKeyField;

        uniqueKeyField = findField(clazz,uniqueKeyName);


        // Extract unique key values from items
        List<Object> uniqueKeyValues = new ArrayList<>();
        Map<Object, Integer> keyToPositionMap = new HashMap<>();

        int position = 0;
        for (T item : items) {
            try {
                uniqueKeyField.setAccessible(true);
                Object keyValue = uniqueKeyField.get(item);
                if (keyValue != null) {
                    uniqueKeyValues.add(keyValue);
                    // Keep track of the original position
                    if (!keyToPositionMap.containsKey(keyValue)) {
                        keyToPositionMap.put(keyValue, position);
                    }
                }
                position++;
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error accessing unique key field", e);
            }
        }

        if (uniqueKeyValues.isEmpty()) {
            return Collections.emptyList();
        }

        // Execute a single query to find all existing items
        Result<Record> existingItems = queryExistingItems(clazz, uniqueKeyField.getName(), uniqueKeyValues);

        List<T> result = new ArrayList<>();
        for (Record record : existingItems) {
            T obj = (T) clazz.getDeclaredConstructor().newInstance();
            for (Field<?> field : record.fields()) {
                if(field.getName()=="location_hierarchy"){
                    continue;
                }
                try {
                    java.lang.reflect.Field objField = findField(clazz,field.getName());
                    objField.setAccessible(true);
                    objField.set(obj, record.get(field));

                } catch (IllegalAccessException e) {
                    System.err.println("Error mapping field: " + field.getName() + " -> " + e.getMessage());
                }
            }
            result.add(obj);
        }

      return result;
    }

    private String extractColumnName(String fieldName, Record record) {
        for (Field<?> dbField : record.fields()) {
            String dbColumnName = dbField.getName(); // This gives "ckunnati.ckuser.userid"

            // Extract only the last part (column name)
            String extractedName = dbColumnName.substring(dbColumnName.lastIndexOf('.') + 1);

            // Match with Java field name
            if (extractedName.equalsIgnoreCase(fieldName)) {
                return dbColumnName; // Return the full qualified name for lookup
            }
        }
        return fieldName; // Fallback if no match found
    }


    private Result<Record> queryExistingItems(Class<? extends CommonDataModel> clazz, String uniqueKeyField, List<Object> uniqueKeyValues) {

        Table<?> table = EntityUtils.getInstance().getDSLContextTable(clazz);
        if (table == null) {
            throw new RuntimeException("DEBUG: Table not found for class: " + clazz.getName());
        }

        // Debugging: Print available table fields
        System.out.println("DEBUG: Available fields in table " + table.getName() + " -> " + Arrays.toString(table.fields()));

        Field<?> uniqueKeyColumn = Arrays.stream(table.fields())
                .filter(f -> f.getName().equalsIgnoreCase(uniqueKeyField)) // Use case-insensitive matching
                .findFirst()
                .orElse(null);

        if (uniqueKeyColumn == null) {
            throw new IllegalArgumentException("DEBUG: Unique key column '" + uniqueKeyField + "' not found in table " + table.getName());
        }

        if (dsl == null) {
            throw new RuntimeException("DEBUG: DSLContext is null");
        }

        System.out.println("DEBUG: Executing query on table " + table.getName() + " with unique key column: " + uniqueKeyColumn.getName());

        Result<Record> list = (Result<Record>) dsl.selectFrom(table)
                .where(uniqueKeyColumn.in(uniqueKeyValues)).fetch();

        return list;
    }

    public List<T> batchSave(List<T> items) {
//        items.forEach(element-> apiFilterAuthorizationManager.assertPermission(element))
        List<T> savedItems = new ArrayList<>();
        List<T> itemsToInsert = new ArrayList<>();
        List<T> existingItems = getExisting(items);
        int count = 0;
        for (T item: items) {
            final T inObject = item;
            String existingHash = item.getHash();
//            TimerUtils.withTime("Time taken to generate Hash "+item.getClass().getName()+":"+item.getId(),
//                    () -> addHash(inObject));
            if (item.canHash() && StringUtils.isNotEmpty(existingHash) && existingHash.equals(item.getHash())) {
                // no need to save this record because this hash is same
                // logger.info("Hash already present in database: {}", existingHash);
            }
            else {
//                T object = fillCommonAttributes(item);
//                T object1 = preSaveEnrichment(object);
                itemsToInsert.add(item);
                count++;
            }
        }
        if(count < 1) return new ArrayList<>();

        Class<? extends CommonDataModel> clazz = itemsToInsert.get(0).getClass();

        Table<?> table = EntityUtils.getInstance().getDSLContextTable(clazz);


        List<Field<?>> fields = getTableFields(dsl, table);
        List<Query> insertQueries = new ArrayList<>();
        for (T item : items) {
            //   Object[] values = getFieldValues(item, fields);
            //    if(item.getLastModifiedTime() == null) item.setLastModifiedTime(new Date());
            Object[] values = getFieldValues(item, fields);

            // Create the base insert query
            Insert<?> insertQuery = dsl.insertInto(table)
                    .columns(fields)
                    .values(values);

            // Convert the query to a SQL string and append ON DUPLICATE KEY UPDATE manually
            String sql = insertQuery.getSQL() + " ON DUPLICATE KEY UPDATE ";

            // Build the update part dynamically
            List<String> updateClauses = new ArrayList<>();
            for (Field<?> field : fields) {
                updateClauses.add(field.getName() + " = VALUES(" + field.getName() + ")");
            }

            sql += String.join(", ", updateClauses);
            List<Object> obj = insertQuery.getBindValues();

            List<Object> bindValues = new ArrayList<>();
            ObjectMapper objectMapper = new ObjectMapper();

            for (Object value : insertQuery.getBindValues()) {
                if (value instanceof ObjectNode) {
                    try {
                        value = objectMapper.writeValueAsString(value);  // Convert to JSON string
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error converting ObjectNode to JSON String", e);
                    }
                }
                bindValues.add(value);
            }

            insertQueries.add(dsl.query(sql, bindValues.toArray()));

        }
        dsl.batch(insertQueries).execute();
        return items;
    }

    private Map<Field<?>, Object> getUpdateMappings(List<Field<?>> fields, Object[] values) {
        Map<Field<?>, Object> updateMappings = new HashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            updateMappings.put(fields.get(i), values[i]);
        }
        return updateMappings;
    }

    private List<Field<?>> getTableFields(DSLContext dslContext, Table<?> table) {
        return Arrays.asList(table.fields()); // Get table column fields dynamically
    }

    private Object[] getFieldValues(Object entity, List<Field<?>> tableFields) {
        List<Object> values = new ArrayList<>();

        for (Field<?> field : tableFields) {
            try {
                java.lang.reflect.Field entityField = findField(entity.getClass(),field.getName());
                entityField.setAccessible(true);
                values.add(entityField.get(entity));
            } catch (IllegalAccessException e) {
                values.add(null); // Handle missing or inaccessible fields gracefully
            }
        }
        return values.toArray();
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        // Try exact match first

        if ("locationHierarchy".equals(toCamelCase(fieldName))) {
            java.lang.reflect.Field stringField = null;
            java.lang.reflect.Field locationField = null;

            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals("locationHierarchy")) {
                    if (field.getType().equals(String.class)) {
                        return field; // Immediately return String type if found
                    } else if (field.getType().equals(Location.class)) {
                        locationField = field; // Store Location field for later
                    }
                }
            }

            // If String type is not found, return Location type as fallback
            if (locationField != null) {
                return locationField;
            }
        }

        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            // Try camelCase version
            String camelCase = toCamelCase(fieldName);
            try {
                return clazz.getDeclaredField(camelCase);
            } catch (NoSuchFieldException ex) {
                // Check superclass if field not found
                Class<?> superClass = clazz.getSuperclass();
                if (superClass != null && !superClass.equals(Object.class)) {
                    return findField(superClass, fieldName);
                }
              //  log.debug("Field not found: {} (or camelCase: {})", fieldName, camelCase);
                return null;
            }
        }
    }

    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;

        for (int i = 0; i < str.length(); i++) {
            char currentChar = str.charAt(i);
            if (currentChar == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(currentChar));
                    nextUpper = false;
                } else {
                    result.append(i == 0 ? Character.toLowerCase(currentChar) : currentChar);
                }
            }
        }

        return result.toString();
    }

}
