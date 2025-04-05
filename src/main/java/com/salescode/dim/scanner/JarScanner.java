package com.salescode.dim.scanner;

import com.salescode.dim.interfaces.TypeAwareEtlStep;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.util.ExceptionUtils;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * JarScanner loads a jar from a given URL or relative file path, scans it for classes that implement {@link TypeAwareEtlStep},
 * and caches instances of these classes.
 */
@Slf4j
public class JarScanner<T extends TypeAwareEtlStep> {

    /**
     * Main method for CLI testing.
     * <p>
     * Usage: java -cp yourclasspath com.salescode.dim.scanner.JarScanner <jar-url-or-relative-path>
     *
     * @param args command-line arguments.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -cp <classpath> com.salescode.dim.scanner.JarScanner <jar-url-or-relative-path>");
            System.exit(1);
        }

        String jarUrl = args[0];

        JarScanner<TypeAwareEtlStep> scanner = new JarScanner<>();
        try {
            Map<String, TypeAwareEtlStep> instanceCache = scanner.loadAndCacheClasses(jarUrl);
            System.out.println("Successfully loaded and cached the following classes:");
            instanceCache.keySet().forEach(System.out::println);
        } catch (Exception e) {
            System.err.println("An error occurred during scanning: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads the jar from the provided URL (e.g., an S3 URL or a relative file path), converts it to a proper `jar:` URL, and caches instances of classes.
     *
     * @param jarUrl the URL or relative file path of the jar file.
     * @return
     */
    public Map<String, T> loadAndCacheClasses(String jarUrl) {
        try {
            URL url = toURL(jarUrl);
            // Convert the URL into a jar URL format: jar:<url>!/
            URL jarURL = new URL("jar:" + url.toExternalForm() + "!/");

            try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarURL}, this.getClass().getClassLoader())) {
                return scanAndCacheClasses(classLoader);
            }
        } catch (Exception e) {
            System.err.println("Error loading classes from jar" + ExceptionUtils.stringifyException(e));
            return Map.of();
        }
    }

    /**
     * Converts the provided jarUrl string into a URL. If it's a relative file path, it will be converted to an absolute file URL.
     *
     * @param jarUrl the jar URL or relative file path.
     * @return the absolute URL.
     * @throws Exception if an error occurs during conversion.
     */
    private URL toURL(String jarUrl) throws Exception {
        // Check if the jarUrl is already an absolute URL (e.g., starts with "http://" or "https://")
        if (jarUrl.startsWith("http://") || jarUrl.startsWith("https://") || jarUrl.startsWith("file:/")) {
            return new URL(jarUrl);
        } else {
            // Assume it is a relative file path.
            File file = new File(jarUrl);
            if (!file.exists()) {
                throw new IllegalArgumentException("File does not exist: " + jarUrl);
            }
            return file.toURI().toURL();
        }
    }

    /**
     * Scans the jar and caches instances of classes.
     *
     * @param classLoader the URLClassLoader to load the classes.
     * @return
     */
    private Map<String, T> scanAndCacheClasses(URLClassLoader classLoader) {
        Map<String, T> instanceCache = new HashMap<>();
        try {
            JarURLConnection jarConn = (JarURLConnection) classLoader.getURLs()[0].openConnection();
            JarFile jarFile = jarConn.getJarFile();
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    String className = entry.getName().replace("/", ".").replace(".class", "");
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        T instance = (T) clazz.getDeclaredConstructor().newInstance();
                        instance.open();
                        instanceCache.put(className, instance);
                        log.info("Loaded and cached class: {}%n", className);
                    } catch (ClassCastException e) {
                        log.error("Class {} does not implement TypeAwareEtlStep: {}%n", className, e.getMessage());
                    } catch (Exception e) {
                        log.error("Error loading class: {}: {}%n", className, e.getMessage());
                    } catch (NoClassDefFoundError e) {
                        log.error("Class not found: {}: {}%n", className, e.getMessage());
                    }
                }
            }
            return instanceCache;
        } catch (Exception e) {
            throw new RuntimeException("Error scanning jar file for classes", e);
        }
    }
}