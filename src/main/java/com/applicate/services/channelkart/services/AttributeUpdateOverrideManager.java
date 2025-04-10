package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AttributeUpdateOverrideManager {

    private Map<String, Map<String,AttributePermission>> permissionMapByLob = new ConcurrentHashMap<>();

    private Map<String, AttributePermission> getPermissionMap() {
        String lob = SecurityContextUtils.getLob();
        if (permissionMapByLob.containsKey(lob)) {
            return permissionMapByLob.get(lob);
        } else {
            Metadata metadata = new MetaDataService().fetchByValue("security", "attributePermissions");
            HashMap<String, AttributePermission> permissionMap = new HashMap<>();
            if (metadata != null) {
                try {
                    ObjectMapper objectMapper = JSONUtils.getObjectMapper();

                    AttributePermission[] permissionsArray = objectMapper.readValue(
                            metadata.getDomainValues().toString(),
                            AttributePermission[].class
                    );

                    for (AttributePermission ap : permissionsArray) {
                        permissionMap.put(ap.getEntityName(), ap);
                    }

                }  catch (JsonMappingException e) {
                    throw new RuntimeException(e);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            permissionMapByLob.put(lob, permissionMap);
            return permissionMap;
        }
    }


    public void overrideAttributes(CommonDataModel currentObject, CommonDataModel dbObject) {
        if (dbObject != null) {
            String entityName = dbObject.getClass().getSimpleName();
            AttributePermission permission = getPermissionMap().get(entityName);
            if (permission != null) {
                String currentuser = SecurityContextUtils.getPrincipal();
                if (currentuser == null || permission.ignorableUsers.contains(currentuser)) {
                    copy(currentObject, dbObject, permission.fieldNames);
                }
            }
        }
    }

    private void copy(CommonDataModel currentObject, CommonDataModel dbObject,
                      List<String> fieldNames) {
        fieldNames.forEach(f -> {
            try {
                Field fi = dbObject.getClass().getDeclaredField(f);
                fi.setAccessible(true);
                Object o = fi.get(dbObject);
                if (o != null && !o.toString().isEmpty()) {
                    fi.set(currentObject, o);
                }
            } catch (Exception e) {
                  System.out.println(e);
            }
        });

    }
    public static class AttributePermission {

        private String entityName;
        private List<String> ignorableUsers;
        private List<String> fieldNames;

        public String getEntityName() {
            return entityName;
        }

        public void setEntityName(String entityName) {
            this.entityName = entityName;
        }

        public List<String> getIgnorableUsers() {
            return ignorableUsers;
        }

        public void setIgnorableUsers(List<String> ignorableUsers) {
            this.ignorableUsers = ignorableUsers;
        }

        public List<String> getFieldNames() {
            return fieldNames;
        }

        public void setFieldNames(List<String> fieldNames) {
            this.fieldNames = fieldNames;
        }

    }
    
}
