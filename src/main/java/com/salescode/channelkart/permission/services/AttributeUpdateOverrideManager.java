package com.salescode.channelkart.permission.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.models.MetaData;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.services.MetaDataService;
import com.salescode.channelkart.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AttributeUpdateOverrideManager {

  private static final Logger log = LoggerFactory.getLogger(AttributeUpdateOverrideManager.class);

  private MetaDataService metadataservice;

  private Map<String, Map<String, AttributePermission>> permissionMapByLob = new ConcurrentHashMap<>();

  public AttributeUpdateOverrideManager(MetaDataService metadataservice) {
    this.metadataservice = metadataservice;
  }

  public void mergeProperties(CommonDataModel currentObject, CommonDataModel dbObject) {
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

  private Map<String, AttributePermission> getPermissionMap() {
    String lob = SecurityContextUtils.getLob();
    if (permissionMapByLob.containsKey(lob)) {
      return permissionMapByLob.get(lob);
    } else {
      MetaData metadata = metadataservice.fetchByValue("security", "attributePermissions");
      HashMap<String, AttributePermission> permissionMap = new HashMap<>();
      if (metadata != null) {
        try {
          JSONUtils.getObjectMapper()
              .readValue(metadata.getDomainValues().toString(),
                  new TypeReference<List<AttributePermission>>() {
                  })
              .stream().forEach(s -> permissionMap.put(s.getEntityName(), s));
        } catch (JsonProcessingException e) {
          log.error("stacktrace", e);
        }
      }
      permissionMapByLob.put(lob, permissionMap);
      return permissionMap;
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
        log.error("stacktrace", e);
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
