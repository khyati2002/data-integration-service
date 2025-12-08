package com.applicate.cokethai.transformer;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class OutletDetailsTransformerTest {
    private static final Logger logger = LoggerFactory.getLogger(OutletDetailsTransformerTest.class);
    
    private OutletDetailsTransformer transformer;
    
    @Before
    public void setUp() {
        transformer = new OutletDetailsTransformer();
    }
    
    @Test
    public void testOutletAddressToStoreaddresssMapping() {
        // Arrange
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("uid", "OUTLET123456");
        inputData.put("type", "LOYALTY");
        inputData.put("custname", "Test Store");
        inputData.put("ownername", "Test Owner");
        inputData.put("outlet_address", "123 Test Street, Test City");
        inputData.put("outlettype", "Retail");
        inputData.put("channeltype", "Direct");
        inputData.put("loyaltytype", "Premium");
        inputData.put("branch", "TEST_BRANCH");
        inputData.put("district", "TEST_DISTRICT");
        
        // Act
        Map<String, Object> result = transformer.transform(inputData);
        
        // Assert
        assertNotNull("Result should not be null", result);
        assertNotNull("Extended attributes should exist", result.get("extendedAttributes"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> extendedAttributes = (Map<String, Object>) result.get("extendedAttributes");
        assertEquals("storeaddresss should be mapped correctly", 
                     "123 Test Street, Test City", 
                     extendedAttributes.get("storeaddresss"));
        
        logger.info("Test passed: outlet_address mapped to storeaddresss successfully");
    }
    
    @Test
    public void testOutletAddressMappingWithNullValue() {
        // Arrange
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("uid", "OUTLET123456");
        inputData.put("type", "LOYALTY");
        inputData.put("custname", "Test Store");
        inputData.put("ownername", "Test Owner");
        inputData.put("outlet_address", null); // null value
        inputData.put("outlettype", "Retail");
        inputData.put("channeltype", "Direct");
        inputData.put("loyaltytype", "Premium");
        inputData.put("branch", "TEST_BRANCH");
        inputData.put("district", "TEST_DISTRICT");
        
        // Act
        Map<String, Object> result = transformer.transform(inputData);
        
        // Assert
        assertNotNull("Result should not be null", result);
        
        // Extended attributes might not exist if no outlet_address is provided
        @SuppressWarnings("unchecked")
        Map<String, Object> extendedAttributes = (Map<String, Object>) result.get("extendedAttributes");
        if (extendedAttributes != null) {
            assertNull("storeaddresss should not be set when outlet_address is null", 
                      extendedAttributes.get("storeaddresss"));
        }
        
        logger.info("Test passed: null outlet_address handled correctly");
    }
    
    @Test
    public void testOutletAddressMappingWithEmptyValue() {
        // Arrange
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("uid", "OUTLET123456");
        inputData.put("type", "LOYALTY");
        inputData.put("custname", "Test Store");
        inputData.put("ownername", "Test Owner");
        inputData.put("outlet_address", "   "); // empty/whitespace value
        inputData.put("outlettype", "Retail");
        inputData.put("channeltype", "Direct");
        inputData.put("loyaltytype", "Premium");
        inputData.put("branch", "TEST_BRANCH");
        inputData.put("district", "TEST_DISTRICT");
        
        // Act
        Map<String, Object> result = transformer.transform(inputData);
        
        // Assert
        assertNotNull("Result should not be null", result);
        
        // Extended attributes might not exist if no valid outlet_address is provided
        @SuppressWarnings("unchecked")
        Map<String, Object> extendedAttributes = (Map<String, Object>) result.get("extendedAttributes");
        if (extendedAttributes != null) {
            assertNull("storeaddresss should not be set when outlet_address is empty", 
                      extendedAttributes.get("storeaddresss"));
        }
        
        logger.info("Test passed: empty outlet_address handled correctly");
    }
    
    @Test
    public void testOutletAddressMappingWithExistingExtendedAttributes() {
        // Arrange
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("uid", "OUTLET123456");
        inputData.put("type", "LOYALTY");
        inputData.put("custname", "Test Store");
        inputData.put("ownername", "Test Owner");
        inputData.put("outlet_address", "456 Another Street, Another City");
        inputData.put("outlettype", "Retail");
        inputData.put("channeltype", "Direct");
        inputData.put("loyaltytype", "Premium");
        inputData.put("branch", "TEST_BRANCH");
        inputData.put("district", "TEST_DISTRICT");
        
        // Act
        Map<String, Object> result = transformer.transform(inputData);
        
        // Assert
        assertNotNull("Result should not be null", result);
        assertNotNull("Extended attributes should exist", result.get("extendedAttributes"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> extendedAttributes = (Map<String, Object>) result.get("extendedAttributes");
        
        // Should have both loyaltyFlag (from existing logic) and storeaddresss (from new logic)
        assertEquals("loyaltyFlag should be preserved", "LOYALTY", extendedAttributes.get("loyaltyFlag"));
        assertEquals("storeaddresss should be mapped correctly", 
                     "456 Another Street, Another City", 
                     extendedAttributes.get("storeaddresss"));
        
        logger.info("Test passed: outlet_address mapped correctly with existing extended attributes");
    }
}