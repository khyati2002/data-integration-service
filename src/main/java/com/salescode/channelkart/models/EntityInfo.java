package com.salescode.channelkart.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public class EntityInfo {

  public static class EntityFieldInfo{

    public String getDataType() {
      return dataType;
    }

    public void setDataType(String dataType) {
      this.dataType = dataType;
    }

    public String getTypeClass() {
      return typeClass;
    }

    public void setTypeClass(String typeClass) {
      this.typeClass = typeClass;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public boolean isPrimitive() {
      return primitive;
    }

    public void setPrimitive(boolean primitive) {
      this.primitive = primitive;
    }

    private String dataType;
    private String typeClass;
    private String name;
    private boolean primitive;
  }

  private String className;

  private String tableName;

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public Map<String, String> getFieldNameMap() {
    return fieldNameMap;
  }

  public void setFieldNameMap(Map<String, String> fieldNameMap) {
    this.fieldNameMap = fieldNameMap;
  }

  public Map<String, EntityFieldInfo> getFieldInfoMap() {
    return fieldInfoMap;
  }

  public void setFieldInfoMap(
      Map<String, EntityFieldInfo> fieldInfoMap) {
    this.fieldInfoMap = fieldInfoMap;
  }

  @JsonIgnore
  private Map<String,String> fieldNameMap;

  private Map<String,EntityFieldInfo> fieldInfoMap;

}
