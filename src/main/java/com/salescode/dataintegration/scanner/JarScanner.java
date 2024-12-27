package com.salescode.dataintegration.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarScanner {
    private static final Logger logger = LoggerFactory.getLogger(JarScanner.class);

    /**
     * Loads the jar from the provided URL (e.g., an S3 URL), converts it to a proper `jar:` URL, and caches instances of classes.
     *
     * @param jarUrl        the S3 URL of the jar file.
     * @param instanceCache the cache to store the class instances.
     */
    public void loadAndCacheClasses(String jarUrl, Map instanceCache) {
        try {
            URL url = new URL(jarUrl);
            URL jarURL = new URL("jar:" + url.toExternalForm() + "!/");

            try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarURL}, this.getClass().getClassLoader())) {
                scanAndCacheClasses(classLoader, instanceCache);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading classes from jar", e);
        }
    }

    /**
     * Scans the jar and caches instances of classes.
     *
     * @param classLoader   the URLClassLoader to load the classes.
     * @param instanceCache the cache to store the class instances.
     */
    private void scanAndCacheClasses(URLClassLoader classLoader, Map instanceCache) {
        try {
            JarURLConnection jarConn = (JarURLConnection) classLoader.getURLs()[0].openConnection();
            JarFile jarFile = jarConn.getJarFile();
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    String className = (entry.getName().split("[.]")[0]).replaceAll("[/]", ".");
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
//                        TypeAwareEtlStep instance = (TypeAwareEtlStep) clazz.getDeclaredConstructor().newInstance();
//                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        instanceCache.put(className, instance);
                        logger.info("Loaded and cached class: {}", className);
                    } catch (Exception e) {
                        logger.error("Error loading class: {}", className, e);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error scanning jar file for classes", e);
        }
    }

}