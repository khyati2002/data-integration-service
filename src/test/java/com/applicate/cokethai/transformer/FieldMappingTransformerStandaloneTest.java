package com.applicate.cokethai.transformer;

import java.util.HashMap;
import java.util.Map;

/**
 * Standalone test for FieldMappingTransformer that doesn't require JUnit
 * This allows us to test the transformation logic without Maven dependencies
 */
public class FieldMappingTransformerStandaloneTest {

    public static void main(String[] args) {
        FieldMappingTransformerStandaloneTest test = new FieldMappingTransformerStandaloneTest();
        
        System.out.println("Running FieldMappingTransformer tests...");
        
        try {
            test.testTransformStoreAddressToOutletAddress();
            test.testTransformStoreIdToOutletId();
            test.testTransformBothFields();
            test.testTransformWithNullInput();
            test.testTransformWithEmptyInput();
            test.testTransformWithNullValues();
            test.testTransformWithSpecialCharacters();
            test.testTransformWithNumericValues();
            
            System.out.println("All tests passed successfully!");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testTransformStoreAddressToOutletAddress() {
        System.out.println("Testing storeaddresss to outlet_address transformation...");
        
        SimpleFieldMappingTransformer transformer = new SimpleFieldMappingTransformer();
        Map<String, Object> input = new HashMap<>();
        input.put("storeaddresss", "123 main street");
        input.put("otherField", "otherValue");

        Map<String, Object> result = transformer.transform(input);

        assert result != null : "Result should not be null";
        assert result.containsKey("outlet_address") : "Result should contain outlet_address";
        assert "123 MAIN STREET".equals(result.get("outlet_address")) : "outlet_address should be capitalized";
        assert !result.containsKey("storeaddresss") : "storeaddresss should be removed";
        assert result.containsKey("otherField") : "Other fields should be preserved";
        assert "otherValue".equals(result.get("otherField")) : "Other field value should be preserved";
        
        System.out.println("✓ storeaddresss to outlet_address test passed");
    }

    private void testTransformStoreIdToOutletId() {
        System.out.println("Testing storeid to outlet_id transformation...");
        
        SimpleFieldMappingTransformer transformer = new SimpleFieldMappingTransformer();
        Map<String, Object> input = new HashMap<>();
        input.put("storeid", "STORE123");
        input.put("otherField", "otherValue");

        Map<String, Object> result = transformer.transform(input);

        assert result != null : "Result should not be null";
        assert result.containsKey("outlet_id") : "Result should contain outlet_id";
        assert "STORE123".equals(result.get("outlet_id")) : "outlet_id should match original storeid";
        assert !result.containsKey("storeid") : "storeid should be removed";
        assert result.containsKey("otherField") : "Other fields should be preserved";
        assert "otherValue".equals(result.get("otherField")) : "Other field value should be preserved";
        
        System.out.println("✓ storeid to outlet_id test passed");
    }

    private void testTransformBothFields() {
        System.out.println("Testing both field transformations...");
        
        SimpleFieldMappingTransformer transformer = new SimpleFieldMappingTransformer();
        Map<String, Object> input = new HashMap<>();
        input.put("storeaddresss", "456 elm street");
        input.put("storeid", "STORE456");
        input.put("preservedField", "preservedValue");

        Map<String, Object> result = transformer.transform(input);

        assert result != null : "Result should not be null";
        assert result.containsKey("outlet_address") : "Result should contain outlet_address";
        assert result.containsKey("outlet_id") : "Result should contain outlet_id";
        assert "456 ELM STREET".equals(result.get("outlet_address")) : "outlet_address should be capitalized";
        assert "STORE456".equals(result.get("outlet_id")) : "outlet_id should match original storeid";
        assert !result.containsKey("storeaddresss") : "storeaddresss should be removed";
        assert !result.containsKey("storeid") : "storeid should be removed";
        assert result.containsKey("preservedField") : "Other fields should be preserved";
        
        System.out.println("✓ Both fields transformation test passed");
    }

    private void testTransformWithNullInput() {
        System.out.println("Testing null input handling...");
        
        SimpleFieldMappingTransformer transformer = new SimpleFieldMappingTransformer();
        Map<String, Object> result = transformer.transform(null);

        assert result != null : "Result should not be null";
        assert result.isEmpty() : "Result should be empty";
        
        System.out.println("✓ Null input test passed");
    }

    private void testTransformWithEmptyInput() {
        System.out.println("Testing empty input handling...");
        
        SimpleFieldMappingTransformer transformer = new SimpleFieldMappingTransformer();
        Map<String, Object> input = new HashMap<>();
        Map<String, Object> result = transformer.transform(input);

        assert result != null : "Result should not be null";
        assert result.isEmpty() : "Result should be empty";
        
        System.out.println("✓ Empty input test passed");
    }

    private void testTransformWithNullValues() {
        System.out.println("Testing null values handling...");
        
        SimpleFieldMappingTransformer transformer = new SimpleFieldMappingTransformer();
        Map<String, Object> input = new HashMap<>();
        input.put("storeaddresss", null);
        input.put("storeid", null);
        input.put("otherField", "value");

        Map<String, Object> result = transformer.transform(input);

        assert result != null : "Result should not be null";
        assert !result.containsKey("outlet_address") : "outlet_address should not be present for null input";
        assert !result.containsKey("outlet_id") : "outlet_id should not be present for null input";
        assert !result.containsKey("storeaddresss") : "storeaddresss should be removed";
        assert !result.containsKey("storeid") : "storeid should be removed";
        assert result.containsKey("otherField") : "Other fields should be preserved";
        
        System.out.println("✓ Null values test passed");
    }

    private void testTransformWithSpecialCharacters() {
        System.out.println("Testing special characters handling...");
        
        SimpleFieldMappingTransformer transformer = new SimpleFieldMappingTransformer();
        Map<String, Object> input = new HashMap<>();
        input.put("storeaddresss", "123 main st, apt #4b");
        input.put("storeid", "STORE-123_ABC");

        Map<String, Object> result = transformer.transform(input);

        assert result != null : "Result should not be null";
        assert "123 MAIN ST, APT #4B".equals(result.get("outlet_address")) : "outlet_address should handle special characters";
        assert "STORE-123_ABC".equals(result.get("outlet_id")) : "outlet_id should preserve special characters";
        
        System.out.println("✓ Special characters test passed");
    }

    private void testTransformWithNumericValues() {
        System.out.println("Testing numeric values handling...");
        
        SimpleFieldMappingTransformer transformer = new SimpleFieldMappingTransformer();
        Map<String, Object> input = new HashMap<>();
        input.put("storeaddresss", 12345);
        input.put("storeid", 67890);

        Map<String, Object> result = transformer.transform(input);

        assert result != null : "Result should not be null";
        assert "12345".equals(result.get("outlet_address")) : "outlet_address should handle numeric input";
        assert "67890".equals(result.get("outlet_id")) : "outlet_id should handle numeric input";
        
        System.out.println("✓ Numeric values test passed");
    }
}