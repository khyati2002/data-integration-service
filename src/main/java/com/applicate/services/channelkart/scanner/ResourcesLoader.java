package com.applicate.services.channelkart.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/***
 * Dhaneesh
 */
@SuppressWarnings("rawtypes")
public class ResourcesLoader {

  private static final Logger log = LoggerFactory.getLogger(ResourcesLoader.class);

  public static class ResourcesInfo{

    public ClassLoader getClassLoader() {
      return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
      this.classLoader = classLoader;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getImplementation() {
      return implementation;
    }

    public void setImplementation(String implementation) {
      this.implementation = implementation;
    }

    public String getLob() {
      return lob;
    }

    public void setLob(String lob) {
      this.lob = lob;
    }

    public Class getClazz() {
      return clazz;
    }

    public ResourcesInfo setClazz(Class clazz) {
      this.clazz = clazz;
      return this;
    }

    private String implementation;

    private ClassLoader classLoader;
    private String type;
    private String lob;

    private Class clazz;

  }
  private Map<String,ResourcesInfo> loaderMap = new ConcurrentHashMap<>();

  public ResourcesLoader(){
    //
  }

  public Map<String, ResourcesInfo> getLoaderMap() {
    return loaderMap;
  }

  public void setLoaderMap(Map<String, ResourcesInfo> loaderMap) {
    this.loaderMap = loaderMap;
  }

  public void load(String resourcePath){
    JarScanner scanner = new JarScanner();
    try {
      Map<String,ResourcesInfo> map = scanner.scan(resourcePath);
      loaderMap.putAll(map);
    } catch (Exception e) {
      log.error("resource load failed with error: " , e);
    }
  }


}
