package com.applicate.services.channelkart.transformers.impl;

import com.bazaarvoice.jolt.Chainr;
import com.salescode.dim.jooq.generated.tables.pojos.TransformerInfo;
import org.jooq.JSON;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test case to verify that the JoltTransformer caches the Chainr instance
 * and that both transform calls use the same cached instance.
 */
public class JoltTransformerTest {

    /**
     * Dummy implementation of TransformerInfo for testing purposes.
     */
    private static class DummyTransformerInfo extends TransformerInfo {
        private String id;

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        public void setCode(String code) {
            super.setCode(JSON.valueOf(code));
        }

    }

    /**
     * A test subclass of JoltTransformer that returns our dummy transformer info.
     */
    private static class TestJoltTransformer extends JoltTransformer {
        private final DummyTransformerInfo transformerInfo;

        public TestJoltTransformer(DummyTransformerInfo transformerInfo) {
            this.transformerInfo = transformerInfo;
        }

        @Override
        public TransformerInfo getTransformerInfo() {
            return transformerInfo;
        }
    }

    @Test
    public void testTransformCachingAndSameChainrInstance() throws Exception {
        // Jolt spec that shifts "foo" to "bar"
        String joltSpec = "[ { \"operation\": \"shift\", \"spec\": { \"foo\": \"bar\" } } ]";

        // Setup dummy transformer info with our dummy code wrapper
        DummyTransformerInfo dummyInfo = new DummyTransformerInfo();
        dummyInfo.setId("testTransformer");
        dummyInfo.setCode(joltSpec);

        // Create an instance of our test transformer
        TestJoltTransformer transformer = new TestJoltTransformer(dummyInfo);

        // Input map that should be transformed: "foo" key is shifted to "bar"
        Map<String, Object> input = new HashMap<>();
        input.put("foo", "value");

        // First transformation call
        Object output1 = transformer.transform(input);
        Assert.assertNotNull("First transform output should not be null", output1);
        Assert.assertTrue("Output should be a Map", output1 instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> outputMap1 = (Map<String, Object>) output1;
        Assert.assertTrue("Output map should contain key 'bar'", outputMap1.containsKey("bar"));
        Assert.assertEquals("Value for 'bar' should be 'value'", "value", outputMap1.get("bar"));

        // Use reflection to access the private templateCompilationCache field
        Field cacheField = JoltTransformer.class.getDeclaredField("templateCompilationCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Chainr> cache = (ConcurrentHashMap<String, Chainr>) cacheField.get(transformer);

        // Retrieve the cached Chainr instance after first transform
        Chainr chainrInstanceFirst = cache.get("testTransformer");
        Assert.assertNotNull("Cached Chainr instance should exist after first transform", chainrInstanceFirst);

        // Second transformation call, which should reuse the cached Chainr instance
        Object output2 = transformer.transform(input);
        Assert.assertNotNull("Second transform output should not be null", output2);
        @SuppressWarnings("unchecked")
        Map<String, Object> outputMap2 = (Map<String, Object>) output2;
        Assert.assertEquals("Value for 'bar' should be 'value'", "value", outputMap2.get("bar"));

        // Retrieve the cached Chainr instance again
        Chainr chainrInstanceSecond = cache.get("testTransformer");
        Assert.assertNotNull("Cached Chainr instance should exist after second transform", chainrInstanceSecond);

        // Verify that both transform calls used the same Chainr instance
        Assert.assertSame("Both transform calls should use the same Chainr instance", chainrInstanceFirst, chainrInstanceSecond);
    }
}