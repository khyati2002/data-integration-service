package com.applicate.cokethai.transformer;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple standalone test for OutletDetailsTransformer
 * This test can be run without Maven dependencies
 */
public class SimpleOutletDetailsTransformerTest {
    
    public static void main(String[] args) {
        System.out.println("Starting OutletDetailsTransformer test...");
        
        try {
            // Create transformer instance
            OutletDetailsTransformer transformer = new OutletDetailsTransformer();
            
            // Test 1: Valid outlet_address mapping
            System.out.println("\n=== Test 1: Valid outlet_address mapping ===");
            Map<String, Object> inputData1 = createTestInput("123 Test Street, Test City");
            Map<String, Object> result1 = transformer.transform(inputData1);
            
            if (result1 != null && result1.containsKey("extendedAttributes")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> extendedAttributes = (Map<String, Object>) result1.get("extendedAttributes");
                String storeaddresss = (String) extendedAttributes.get("storeaddresss");
                
                if ("123 Test Street, Test City".equals(storeaddresss)) {
                    System.out.println("✓ PASS: outlet_address correctly mapped to storeaddresss");
                    System.out.println("  Expected: 123 Test Street, Test City");
                    System.out.println("  Actual: " + storeaddresss);
                } else {
                    System.out.println("✗ FAIL: outlet_address mapping incorrect");
                    System.out.println("  Expected: 123 Test Street, Test City");
                    System.out.println("  Actual: " + storeaddresss);
                }
            } else {
                System.out.println("✗ FAIL: extendedAttributes not found in result");
            }
            
            // Test 2: Null outlet_address
            System.out.println("\n=== Test 2: Null outlet_address ===");
            Map<String, Object> inputData2 = createTestInput(null);
            Map<String, Object> result2 = transformer.transform(inputData2);
            
            if (result2 != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> extendedAttributes = (Map<String, Object>) result2.get("extendedAttributes");
                if (extendedAttributes == null || !extendedAttributes.containsKey("storeaddresss")) {
                    System.out.println("✓ PASS: null outlet_address handled correctly (storeaddresss not set)");
                } else {
                    System.out.println("✗ FAIL: null outlet_address should not set storeaddresss");
                    System.out.println("  Found storeaddresss: " + extendedAttributes.get("storeaddresss"));
                }
            } else {
                System.out.println("✗ FAIL: transformer returned null result");
            }
            
            // Test 3: Empty outlet_address
            System.out.println("\n=== Test 3: Empty outlet_address ===");
            Map<String, Object> inputData3 = createTestInput("   ");
            Map<String, Object> result3 = transformer.transform(inputData3);
            
            if (result3 != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> extendedAttributes = (Map<String, Object>) result3.get("extendedAttributes");
                if (extendedAttributes == null || !extendedAttributes.containsKey("storeaddresss")) {
                    System.out.println("✓ PASS: empty outlet_address handled correctly (storeaddresss not set)");
                } else {
                    System.out.println("✗ FAIL: empty outlet_address should not set storeaddresss");
                    System.out.println("  Found storeaddresss: " + extendedAttributes.get("storeaddresss"));
                }
            } else {
                System.out.println("✗ FAIL: transformer returned null result");
            }
            
            System.out.println("\n=== Test Summary ===");
            System.out.println("All tests completed. Check results above for pass/fail status.");
            
        } catch (Exception e) {
            System.err.println("Test failed with exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static Map<String, Object> createTestInput(String outletAddress) {
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("uid", "OUTLET123456");
        inputData.put("type", "LOYALTY");
        inputData.put("custname", "Test Store");
        inputData.put("ownername", "Test Owner");
        inputData.put("outlet_address", outletAddress);
        inputData.put("outlettype", "Retail");
        inputData.put("channeltype", "Direct");
        inputData.put("loyaltytype", "Premium");
        inputData.put("branch", "TEST_BRANCH");
        inputData.put("district", "TEST_DISTRICT");
        return inputData;
    }
}