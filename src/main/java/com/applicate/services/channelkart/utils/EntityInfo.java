package com.applicate.services.channelkart.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class EntityInfo {

  @Setter
  @Getter
  public static class EntityFieldInfo{

      private String dataType;
    private String typeClass;
    private String name;
    private boolean primitive;
  }

  private String className;

  private String tableName;

    @JsonIgnore
  private Map<String,String> fieldNameMap;

  private Map<String,EntityFieldInfo> fieldInfoMap;

}
