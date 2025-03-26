package com.applicate.services.channelkart.utils;

import com.applicate.services.channelkart.models.CommonDataModel;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.UUID;

public class IDGenerator {

    // Method to get all fields of a class including inherited fields
    private Field[] getAllFields(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null) {
            Field[] superFields = getAllFields(superClazz);
            Field[] combinedFields = new Field[fields.length + superFields.length];
            System.arraycopy(fields, 0, combinedFields, 0, fields.length);
            System.arraycopy(superFields, 0, combinedFields, fields.length, superFields.length);
            return combinedFields;
        }
        return fields;
    }


    private StringBuilder generateDynamicIds(Field[] fields, CommonDataModel cdm, ArrayNode columnArr) throws Exception {
        StringBuilder dynamicId = new StringBuilder();
        for (int i = 0; i < columnArr.size(); i++) {
            String columnName = columnArr.get(i).asText();
            appendFieldValueToId(fields, cdm, columnName,dynamicId);
        }
        return dynamicId;
    }


    private void appendFieldValueToId(Field[] fields, CommonDataModel cdm, String columnName,StringBuilder dynamicId) throws Exception {
        for (Field field : fields) {
            if (field.getName().equalsIgnoreCase(columnName)) {
                try {
                    field.setAccessible(true); // Access private fields
                    Object value = field.get(cdm);
                    appendValue(dynamicId, value);
                } catch (IllegalAccessException e) {
                    throw new Exception("Failed to access field '" + field.getName() + "' in class '" + cdm.getClass().getSimpleName() + "'", e);
                }
            }
        }
    }


    private void appendValue(StringBuilder dynamicId, Object value) {
        String valueStr = (value != null) ? value.toString() : "";
        if (dynamicId.length() == 0) {
            dynamicId.append(valueStr);
        } else {
            dynamicId.append("-").append(valueStr);
        }
    }


    public String getIdWithMetaData(CommonDataModel cdm, JsonNode metaData) {
        try {
            ArrayNode columnArr = JSONUtils.getObjectMapper().createArrayNode();
            if (metaData != null) {
                columnArr = (ArrayNode) metaData;
                columnArr = (ArrayNode) columnArr.get(0).get("dynamicKeys");
            }

            Field[] fields = getAllFields(cdm.getClass());

            if (columnArr != null && columnArr.size() > 0) {
                StringBuilder dynamicId = generateDynamicIds(fields, cdm, columnArr);

                if (dynamicId.length() != 0) {
                    return dynamicId.toString().toLowerCase().replace(" ", "-");
                } else {
                    return UUID.randomUUID().toString();
                }
            }
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
        return UUID.randomUUID().toString();
    }


    public String getMd5(String input) throws Exception {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashtext = new StringBuilder(no.toString(16));
            while (hashtext.length() < 32) {
                hashtext.append("0").append(hashtext);
            }
            return hashtext.toString();
        } catch (Exception e) {
            throw new Exception(e);
        }
    }
}
