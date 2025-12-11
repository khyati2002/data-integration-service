package com.salescode.dim.scanner;

import com.salescode.dim.interfaces.TypeAwareEtlStep;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Map;

public class JarScannerTest {

    /**
     * This test expects that a jar file named "dummy-etl-step.jar" exists in the "lib" directory.
     * That jar should contain at least one class implementing TypeAwareEtlStep.
     */
    @Test
    public void testLoadAndCacheClasses() {
        // Locate the jar file from the lib directory.
        File jarFile = new File("lib/bundle.jar");
        Assert.assertTrue("Test jar file not found: " + jarFile.getAbsolutePath(), jarFile.exists());

        // Convert the jar file to a URL string.
        // JarScanner will prepend "jar:" and append "!/" to form the proper jar URL.
        String jarUrl = jarFile.toURI().toASCIIString();

        // Prepare a cache for storing instances.

        // Create an instance of JarScanner and load classes.
        JarScanner<TypeAwareEtlStep> jarScanner = new JarScanner<>();

        Map<String, TypeAwareEtlStep> instanceCache = jarScanner.loadAndCacheClasses(jarUrl);

        // Validate that the cache is populated.
        Assert.assertFalse("Instance cache should not be empty", instanceCache.isEmpty());

        // Optionally, check that all cached instances implement TypeAwareEtlStep.
        for (Map.Entry<String, TypeAwareEtlStep> entry : instanceCache.entrySet()) {
            Assert.assertNotNull("Loaded instance is null for key: " + entry.getKey(), entry.getValue());
            Assert.assertTrue("Instance does not implement TypeAwareEtlStep: " + entry.getKey(), entry.getValue() instanceof TypeAwareEtlStep);
        }
    }
}