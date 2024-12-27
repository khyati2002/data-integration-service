package com.salescode.channelkart.cache;

import java.io.Serializable;

public class CacheUpdateEvent implements Serializable {

  private static final long serialVersionUID = 7271540090699595148L;

  private String lob;

  private String domainName;

  private String key;

  private boolean clearAll;

  public String getLob() {
    return lob;
  }

  public void setLob(String lob) {
    this.lob = lob;
  }

  public String getDomainName() {
    return domainName;
  }

  public void setDomainName(String domainName) {
    this.domainName = domainName;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public boolean isClearAll() {
    return clearAll;
  }

  public void setClearAll(boolean clearAll) {
    this.clearAll = clearAll;
  }

  @Override
  public String toString() {
    return "CacheUpdateEvent{" +
            "lob='" + lob + '\'' +
            ", domainName='" + domainName + '\'' +
            ", key='" + key + '\'' +
            ", clearAll=" + clearAll +
            '}';
  }
}
