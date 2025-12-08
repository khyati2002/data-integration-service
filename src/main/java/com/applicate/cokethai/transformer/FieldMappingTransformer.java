package com.applicate.cokethai.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.commons.collections.MapUtils.getString;

/**
 * Transformer to convert mapped field names to actual field names.
 * Handles conversion of:
 * - storeaddresss -> outlet_address (with capitalization)
 * - storeid -> outlet_id
 */
public class FieldMappingTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    private static final Logger logger = LoggerFactory.getLogger(FieldMappingTransformer.class);

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        if (inputMap == null) {
            logger.warn("Input map is null, returning empty map");
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
                logger.debug("Transformed storeaddresss '{}' to outlet_address '{}'", storeAddress, capitalizedAddress);
            }
            // Always remove the original mapped field if it exists
            if (inputMap.containsKey("storeaddresss")) {
                output.remove("storeaddresss");
            }

            // Transform storeid to outlet_id
            String storeId = processFieldValue(inputMap.get("storeid"));
            if (storeId != null) {
                output.put("outlet_id", storeId);
                logger.debug("Transformed storeid '{}' to outlet_id '{}'", storeId, storeId);
            }
            // Always remove the original mapped field if it exists
            if (inputMap.containsKey("storeid")) {
                output.remove("storeid");
            }

        } catch (Exception e) {
            logger.error("Error during field mapping transformation", e);
            // Return original input in case of error to maintain data integrity
            return inputMap;
        }

        return output;
    }

    /**
     * Validates input data before transformation
     * @param inputMap the input data map
     * @return true if input is valid for transformation
     */
    private boolean validateInput(Map<String, Object> inputMap) {
        if (inputMap == null) {
            logger.warn("Input map is null");
            return false;
        }

        // Check if at least one of the expected mapped fields exists
        boolean hasStoreAddress = inputMap.containsKey("storeaddresss");
        boolean hasStoreId = inputMap.containsKey("storeid");

        if (!hasStoreAddress && !hasStoreId) {
            logger.debug("No mapped fields found in input data");
            return false;
        }

        return true;
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