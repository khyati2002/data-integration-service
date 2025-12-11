package com.applicate.cokethai.transformer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simplified version of FieldMappingTransformer for testing without external dependencies
 * Transformer to convert mapped field names to actual field names.
 * Handles conversion of:
 * - storeaddresss -> outlet_address (with capitalization)
 * - storeid -> outlet_id
 */
public class SimpleFieldMappingTransformer {

    public Map<String, Object> transform(Map<String, Object> inputMap) {
        if (inputMap == null) {
            System.out.println("Input map is null, returning empty map");
            return new LinkedHashMap<>();
        }

        Map<String, Object> output = new LinkedHashMap<>(inputMap);

        try {
            // Transform storeaddresss to outlet_address with capitalization
            String storeAddress = processFieldValue(inputMap.get("storeaddresss"));
            if (storeAddress != null) {
                // Convert to uppercase as per requirement
                String capitalizedAddress = storeAddress.toUpperCase();
                output.put("outlet_address", capitalizedAddress);
                System.out.println("Transformed storeaddresss '" + storeAddress + "' to outlet_address '" + capitalizedAddress + "'");
            }
            // Always remove the original mapped field if it exists
            if (inputMap.containsKey("storeaddresss")) {
                output.remove("storeaddresss");
            }

            // Transform storeid to outlet_id
            String storeId = processFieldValue(inputMap.get("storeid"));
            if (storeId != null) {
                output.put("outlet_id", storeId);
                System.out.println("Transformed storeid '" + storeId + "' to outlet_id '" + storeId + "'");
            }
            // Always remove the original mapped field if it exists
            if (inputMap.containsKey("storeid")) {
                output.remove("storeid");
            }

        } catch (Exception e) {
            System.err.println("Error during field mapping transformation: " + e.getMessage());
            // Return original input in case of error to maintain data integrity
            return inputMap;
        }

        return output;
    }

    /**
     * Handles null and edge cases for field values
     * @param value the field value to process
     * @return processed value or null if invalid
     */
    private String processFieldValue(Object value) {
        if (value == null) {
            return null;
        }

        String stringValue = value.toString().trim();
        
        // Handle empty strings
        if (stringValue.isEmpty()) {
            return null;
        }

        return stringValue;
    }
}