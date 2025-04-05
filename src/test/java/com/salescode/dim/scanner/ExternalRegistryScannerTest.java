package com.salescode.dim.scanner;

import com.salescode.dim.interfaces.TypeAwareEtlStep;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Properties;

public class ExternalRegistryScannerTest {

    private ExternalRegistryScanner scanner;

    @Before
    public void setUp() {
        // Configure properties for the test.
        Properties props = new Properties();
        // Adjust the path to point to your test bundle jar file.
        props.setProperty("bundle.relative.path", "lib/bundle.jar");
        props.setProperty("lob", "testlob");

        // Create an instance of ExternalRegistryScanner.
        scanner = ExternalRegistryScanner.createInstance(props);
    }

    @Test
    public void testLoadInstancesNotEmpty() {
        // Retrieve the cached instances.
        Collection<? extends TypeAwareEtlStep> instances = scanner.getEtlInstances();
        // Verify that at least one instance is loaded.
        Assert.assertFalse("Instance cache should not be empty", instances.isEmpty());
    }
}