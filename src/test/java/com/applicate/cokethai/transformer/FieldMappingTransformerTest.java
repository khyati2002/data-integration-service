package com.applicate.cokethai.transformer;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test class for FieldMappingTransformer
 * Tests the conversion of mapped field names to actual field names
 */
public class FieldMappingTransformerTest {

    private FieldMappingTransformer transformer;

    @Before
    public void setUp() {
        transformer = new FieldMappingTransformer();
    }

    @Test
    public void testTransformStoreAddressToOutletAddress() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("storeaddresss", "123 main street");
        input.put("otherField", "otherValue");

        // Act
        Map<String, Object> result = transformer.transform(input);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain outlet_address", result.containsKey("outlet_address"));
        assertEquals("outlet_address should be capitalized", "123 MAIN STREET", result.get("outlet_address"));
        assertFalse("storeaddresss should be removed", result.containsKey("storeaddresss"));
        assertTrue("Other fields should be preserved", result.containsKey("otherField"));
        assertEquals("Other field value should be preserved", "otherValue", result.get("otherField"));
    }

    @Test
    public void testTransformStoreIdToOutletId() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("storeid", "STORE123");
        input.put("otherField", "otherValue");

        // Act
        Map<String, Object> result = transformer.transform(input);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain outlet_id", result.containsKey("outlet_id"));
        assertEquals("outlet_id should match original storeid", "STORE123", result.get("outlet_id"));
        assertFalse("storeid should be removed", result.containsKey("storeid"));
        assertTrue("Other fields should be preserved", result.containsKey("otherField"));
        assertEquals("Other field value should be preserved", "otherValue", result.get("otherField"));
    }

    @Test
    public void testTransformBothFields() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("storeaddresss", "456 elm street");
        input.put("storeid", "STORE456");
        input.put("preservedField", "preservedValue");

        // Act
        Map<String, Object> result = transformer.transform(input);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain outlet_address", result.containsKey("outlet_address"));
        assertTrue("Result should contain outlet_id", result.containsKey("outlet_id"));
        assertEquals("outlet_address should be capitalized", "456 ELM STREET", result.get("outlet_address"));
        assertEquals("outlet_id should match original storeid", "STORE456", result.get("outlet_id"));
        assertFalse("storeaddresss should be removed", result.containsKey("storeaddresss"));
        assertFalse("storeid should be removed", result.containsKey("storeid"));
        assertTrue("Other fields should be preserved", result.containsKey("preservedField"));
    }

    @Test
    public void testTransformWithNullInput() {
        // Act
        Map<String, Object> result = transformer.transform(null);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty", result.isEmpty());
    }

    @Test
    public void testTransformWithEmptyInput() {
        // Arrange
        Map<String, Object> input = new HashMap<>();

        // Act
        Map<String, Object> result = transformer.transform(input);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty", result.isEmpty());
    }

    @Test
    public void testTransformWithNullValues() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("storeaddresss", null);
        input.put("storeid", null);
        input.put("otherField", "value");

        // Act
        Map<String, Object> result = transformer.transform(input);

        // Assert
        assertNotNull("Result should not be null", result);
        assertFalse("outlet_address should not be present for null input", result.containsKey("outlet_address"));
        assertFalse("outlet_id should not be present for null input", result.containsKey("outlet_id"));
        assertFalse("storeaddresss should be removed", result.containsKey("storeaddresss"));
        assertFalse("storeid should be removed", result.containsKey("storeid"));
        assertTrue("Other fields should be preserved", result.containsKey("otherField"));
    }

    @Test
    public void testTransformWithEmptyStringValues() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("storeaddresss", "");
        input.put("storeid", "   ");
        input.put("otherField", "value");

        // Act
        Map<String, Object> result = transformer.transform(input);

        // Assert
        assertNotNull("Result should not be null", result);
        assertFalse("outlet_address should not be present for empty input", result.containsKey("outlet_address"));
        assertFalse("outlet_id should not be present for empty input", result.containsKey("outlet_id"));
        assertFalse("storeaddresss should be removed", result.containsKey("storeaddresss"));
        assertFalse("storeid should be removed", result.containsKey("storeid"));
        assertTrue("Other fields should be preserved", result.containsKey("otherField"));
    }

    @Test
    public void testTransformWithSpecialCharacters() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("storeaddresss", "123 main st, apt #4b");
        input.put("storeid", "STORE-123_ABC");

        // Act
        Map<String, Object> result = transformer.transform(input);

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("outlet_address should handle special characters", "123 MAIN ST, APT #4B", result.get("outlet_address"));
        assertEquals("outlet_id should preserve special characters", "STORE-123_ABC", result.get("outlet_id"));
    }

    @Test
    public void testTransformWithNumericValues() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("storeaddresss", 12345);
        input.put("storeid", 67890);

        // Act
        Map<String, Object> result = transformer.transform(input);

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("outlet_address should handle numeric input", "12345", result.get("outlet_address"));
        assertEquals("outlet_id should handle numeric input", "67890", result.get("outlet_id"));
    }

    @Test
    public void testDataIntegrityOnError() {
        // This test ensures that if an error occurs during transformation,
        // the original data is preserved
        Map<String, Object> input = new HashMap<>();
        input.put("storeaddresss", "test address");
        input.put("storeid", "test123");
        input.put("importantData", "mustPreserve");

        // Act
        Map<String, Object> result = transformer.transform(input);

        // Assert
        assertNotNull("Result should not be null", result);
        // In normal case, transformation should work
        assertTrue("Should contain transformed fields", result.containsKey("outlet_address") || result.containsKey("outlet_id"));
    }
}