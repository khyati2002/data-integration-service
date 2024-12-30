package com.salescode.channelkart.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.channelkart.converters.JSONObjectConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;


@Entity
@Table(name = "ck_generic_schema", indexes = {
    @Index(name = "ck_index_name", columnList = "name")
})
@JsonInclude(Include.NON_NULL)
public class GenericSchema extends CommonDataModel {
  /**
  id,name,timestamp as default in all schema
   {
      name:"ck_new",
      spec:{
        one:{
         type: String,
         key:"key1",
        },
        two:{
         type: Number,
         rangeKey:true,
        }
      }
   }
   */
  private static final long serialVersionUID = 1L;

  private String name;

  public JsonNode getSpec() {
    return spec;
  }

  public void setSpec(JsonNode spec) {
    this.spec = spec;
  }


  @Column(columnDefinition = "json")
  @Convert(converter= JSONObjectConverter.class)
  private JsonNode spec;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
