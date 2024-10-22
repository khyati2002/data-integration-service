package com.salescode.channelkart.scanner;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/***
 * Dhaneesh
 */
@Getter
@Setter
@SuppressWarnings("rawtypes")
public class ResourcesLoader {

    private static final Logger log = LoggerFactory.getLogger(ResourcesLoader.class);
    private final Map<String, ResourcesInfo> loaderMap = new ConcurrentHashMap<>();

    public ResourcesLoader() {
    }

    public void load(String resourcePath) {
        JarScanner scanner = new JarScanner();
        try {
            Map<String, ResourcesInfo> map = scanner.scan(resourcePath);
            loaderMap.putAll(map);
        } catch (Exception e) {
            log.error("resource load failed with error: ", e);
        }
    }

    @Getter
    @Setter
    public static class ResourcesInfo {

        private String implementation;
        private ClassLoader classLoader;
        private String type;
        private String lob;
        private Class clazz;

        public ResourcesInfo setClazz(Class clazz) {
            this.clazz = clazz;
            return this;
        }

    }


}
