package com.salescode.dim.scanner;

import com.salescode.dim.interfaces.TypeAwareEtlStep;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ExternalRegistryScanner implements Serializable {

    private static final long serialVersionUID = -7659444822219009942L;

    private static final JarScanner<TypeAwareEtlStep> jarScanner = new JarScanner<>();

    private static Map<String, TypeAwareEtlStep> instanceCache = new ConcurrentHashMap<>();

    private static ExternalRegistryScanner instance;

    private ExternalRegistryScanner(Properties properties) {
        String externalRegistryPath = properties.getProperty("bundle.relative.path", "lib/bundle.jar");
        log.info("Loading external registry from {}", externalRegistryPath);
        InputStream jarStream = ExternalRegistryScanner.class.getClassLoader().getResourceAsStream(externalRegistryPath);
        try {
            Path tempJar = Files.createTempFile("embedded-lib-", ".jar");
            Files.copy(Objects.requireNonNull(jarStream), tempJar, StandardCopyOption.REPLACE_EXISTING);
            jarStream.close();
            String lob = properties.getProperty("lob");
            instanceCache = loadClassesFromLob(String.format(tempJar.toAbsolutePath().toString(), lob));
        } catch (Exception e) {
            log.error("Failed to load external registry", e);
        }
    }

    public static synchronized ExternalRegistryScanner createInstance(Properties properties) {
        if (instance == null) {
            instance = new ExternalRegistryScanner(properties);
        }
        return instance;
    }

    public static synchronized ExternalRegistryScanner getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ExternalRegistryScanner has not been initialized yet");
        }
        return instance;
    }

    /**
     * Loads and caches classes from a bundle jar file based on the LOB.
     *
     * @param externalRegistryPath the path of the external registry.
     * @return a map of class names to their TypeAwareEtlStep instances.
     */
    public Map<String, TypeAwareEtlStep> loadClassesFromLob(String externalRegistryPath) {
        return jarScanner.loadAndCacheClasses(externalRegistryPath);
    }

    /**
     * Retrieve a cached instance of a class by its name.
     */
    public Object getCachedInstance(String className) {
        return instanceCache.get(className);
    }

    public Collection<TypeAwareEtlStep> getEtlInstances() {
        return instanceCache.values();
    }

    /**
     * Ensures that deserialization preserves the singleton property.
     */
    private Object readResolve() throws ObjectStreamException {
        return getInstance();
    }
}