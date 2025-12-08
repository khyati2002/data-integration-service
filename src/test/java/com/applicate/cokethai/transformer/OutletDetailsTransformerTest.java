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
    
    // Tests for outlet_address to storeaddresss mapping (my implementation)
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
    
    // Tests for outlet_id to storeid mapping (from remote branch)
    @Test
    public void testFieldMappingOutletIdToStoreid() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("outlet_id", "STORE123");
        input.put("uid", "UID456");
        input.put("type", "LOYALTY");
        input.put("custname", "Test Store");
        input.put("ownername", "Test Owner");
        
        // Act
        Map<String, Object> result = transformer.transform(input);
        
        // Assert
        assertEquals("STORE123", result.get("storeid"));
        assertNull(result.get("outletCode")); // Should not be set when outlet_id is present
        
        // Verify userName mapping
        @SuppressWarnings("unchecked")
        Map<String, Object> userName = (Map<String, Object>) result.get("userName");
        assertNotNull(userName);
        assertEquals("STORE123", userName.get("loginId"));
        assertEquals("STORE123", userName.get("userAccountId"));
    }
    
    @Test
    public void testFieldMappingOutletAddressToStoreaddresssAlternative() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("outlet_address", "123 Main Street, City, State");
        input.put("uid", "UID456");
        input.put("type", "LOYALTY");
        input.put("custname", "Test Store");
        input.put("ownername", "Test Owner");
        
        // Act
        Map<String, Object> result = transformer.transform(input);
        
        // Assert - Check if it's in extendedAttributes (my implementation) or direct field (remote implementation)
        @SuppressWarnings("unchecked")
        Map<String, Object> extendedAttributes = (Map<String, Object>) result.get("extendedAttributes");
        if (extendedAttributes != null && extendedAttributes.containsKey("storeaddresss")) {
            assertEquals("123 Main Street, City, State", extendedAttributes.get("storeaddresss"));
        } else {
            assertEquals("123 Main Street, City, State", result.get("storeaddresss"));
        }
    }
    
    @Test
    public void testFallbackToUidWhenOutletIdNotPresent() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("uid", "UID456");
        input.put("type", "LOYALTY");
        input.put("custname", "Test Store");
        input.put("ownername", "Test Owner");
        
        // Act
        Map<String, Object> result = transformer.transform(input);
        
        // Assert
        assertEquals("UID456", result.get("outletCode"));
        assertNull(result.get("storeid")); // Should not be set when outlet_id is not present
        
        // Verify userName mapping
        @SuppressWarnings("unchecked")
        Map<String, Object> userName = (Map<String, Object>) result.get("userName");
        assertNotNull(userName);
        assertEquals("UID456", userName.get("loginId"));
        assertEquals("UID456", userName.get("userAccountId"));
    }
    
    @Test
    public void testBothOutletIdAndOutletAddressMapping() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("outlet_id", "STORE123");
        input.put("outlet_address", "123 Main Street, City, State");
        input.put("uid", "UID456");
        input.put("type", "LOYALTY");
        input.put("custname", "Test Store");
        input.put("ownername", "Test Owner");
        
        // Act
        Map<String, Object> result = transformer.transform(input);
        
        // Assert
        assertEquals("STORE123", result.get("storeid"));
        
        // Check if storeaddresss is in extendedAttributes (my implementation) or direct field (remote implementation)
        @SuppressWarnings("unchecked")
        Map<String, Object> extendedAttributes = (Map<String, Object>) result.get("extendedAttributes");
        if (extendedAttributes != null && extendedAttributes.containsKey("storeaddresss")) {
            assertEquals("123 Main Street, City, State", extendedAttributes.get("storeaddresss"));
        } else {
            assertEquals("123 Main Street, City, State", result.get("storeaddresss"));
        }
        
        assertNull(result.get("outletCode")); // Should not be set when outlet_id is present
    }
}