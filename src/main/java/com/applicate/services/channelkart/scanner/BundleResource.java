package com.applicate.services.channelkart.scanner;

/***
 * Dhaneesh
 */
@SuppressWarnings("rawtypes")
public class BundleResource {

  private String type;

  private String implementation;

  private String status="Active";

  private ClassLoader loader;

  private Class<Object> clazz;

  private String lob;

  public String getLob() {
    return lob;
  }

  public void setLob(String lob) {
    this.lob = lob;
  }

  public String getType() {
    return type;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getImplementation() {
    return implementation;
  }

  public Class<Object> getClazz() {
    return clazz;
  }

  public BundleResource setClazz(Class<Object> clazz) {
    this.clazz = clazz;
    return this;
  }

  public ClassLoader getLoader() {
    return loader;
  }

  public BundleResource setLoader(ClassLoader loader) {
    this.loader = loader;
    return this;
  }

  public void setImplementation(String implementation) {
    this.implementation = implementation;
  }
}
