package com.applicate.services.channelkart.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.applicate.services.channelkart.converters.JSONObjectConverter;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;


@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Table(name = "ck_generic_object", indexes = {
    @Index(name = "ck_index_name", columnList = "name"),
    @Index(name = "ck_index_loginid", columnList = "loginId"),
    @Index(name = "ck_index_key1", columnList = "key1"),
    @Index(name = "ck_index_key2", columnList = "key2"),
    @Index(name = "ck_index_key3", columnList = "key3"),
    @Index(name = "ck_index_key4", columnList = "key4"),
    @Index(name = "ck_index_key5", columnList = "key5"),
    @Index(name = "ck_index_key6", columnList = "key6"),
    @Index(name = "ck_index_key7", columnList = "key7"),
    @Index(name = "ck_index_key8", columnList = "key8"),
    @Index(name = "ck_index_key9", columnList = "key9"),
    @Index(name = "ck_index_key10", columnList = "key10"),
    @Index(name = "ck_index_timestamp", columnList = "timestamp"),
    @Index(name = "ck_index_rangekey", columnList = "rangeKey"),

    @Index(name = "ck_index_hierarchy", columnList = "hierarchy")
})
public class GenericEntity extends CommonDataModel {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private String name;

  private String loginId;

  private String key1;

  private String key2;

  private String key3;

  private String key4;

  private String key5;
  
  private String key6;

  private String key7;

  private String key8;

  private String key9;

  private String key10;

  private Date date;

  private long timestamp;

  public long getRangeKey() {
    return rangeKey;
  }

  public void setRangeKey(long rangeKey) {
    this.rangeKey = rangeKey;
  }

  private String hierarchy;

  private long rangeKey;

  public String getHierarchy() {
    return hierarchy;
  }

  public void setHierarchy(String hierarchy) {
    this.hierarchy = hierarchy;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String getKey1() {
    return key1;
  }

  public void setKey1(String key1) {
    this.key1 = key1;
  }

  public String getKey2() {
    return key2;
  }

  public void setKey2(String key2) {
    this.key2 = key2;
  }

  public String getKey3() {
    return key3;
  }

  public void setKey3(String key3) {
    this.key3 = key3;
  }

  public String getKey4() {
    return key4;
  }

  public void setKey4(String key4) {
    this.key4 = key4;
  }

  public String getKey5() {
    return key5;
  }

  public void setKey5(String key5) {
    this.key5 = key5;
  }

  public String getKey7() {
        return key7;
    }
  public void setKey7(String key7) {
        this.key7 = key7;
    }

  public String getKey8() {
        return key8;
    }
  public void setKey8(String key8) {
        this.key8 = key8;
    }
  public String getKey9() { return key9; }

  public void setKey9(String key9){ this.key9 = key9;}

  public String getKey10() { return key10;}

  public void setKey10(String key10) {this.key10 = key10;}

  public JsonNode getPayload() {
    return payload;
  }

  public void setPayload(JsonNode payload) {
    this.payload = payload;
  }

  @Column(columnDefinition = "json")
  @Convert(converter= JSONObjectConverter.class)
  private JsonNode payload;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLoginId() {
    return loginId;
  }

  public void setLoginId(String loginId) {
    this.loginId = loginId;
  }

public String getKey6() {
	return key6;
}

public void setKey6(String key6) {
	this.key6 = key6;
}

public Date getDate() {
	return date;
}

public void setDate(Date date) {
	this.date = date;
}

@Override
public int hashCode() {
	final int prime = 31;
	int result = super.hashCode();
	result = prime * result
			+ Objects.hash(hierarchy, key1, key2, key3, key4, key5, key6, loginId, name, payload, rangeKey);
	return result;
}

@Override
public boolean equals(Object obj) {
	if (this == obj) {
		return true;
	}
	if (!super.equals(obj)) {
		return false;
	}
	if (!(obj instanceof GenericEntity)) {
		return false;
	}
	GenericEntity other = (GenericEntity) obj;
	return Objects.equals(hierarchy, other.hierarchy) && Objects.equals(key1, other.key1)
			&& Objects.equals(key2, other.key2) && Objects.equals(key3, other.key3) && Objects.equals(key4, other.key4)
			&& Objects.equals(key5, other.key5) && Objects.equals(key6, other.key6)
			&& Objects.equals(loginId, other.loginId) && Objects.equals(name, other.name)
			&& Objects.equals(payload, other.payload) && rangeKey == other.rangeKey;
}

}
