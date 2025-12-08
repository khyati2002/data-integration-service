package com.applicate.cokethai.transformer;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

public class OutletDetailsTransformerTest {
    
    private OutletDetailsTransformer transformer;
    
    @Before
    public void setUp() {
        transformer = new OutletDetailsTransformer();
    }
    
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
    public void testFieldMappingOutletAddressToStoreaddresss() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("outlet_address", "123 Main Street, City, State");
        input.put("uid", "UID456");
        input.put("type", "LOYALTY");
        input.put("custname", "Test Store");
        input.put("ownername", "Test Owner");
        
        // Act
        Map<String, Object> result = transformer.transform(input);
        
        // Assert
        assertEquals("123 Main Street, City, State", result.get("storeaddresss"));
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
    public void testEmptyOutletIdFallsBackToUid() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("outlet_id", "");
        input.put("uid", "UID456");
        input.put("type", "LOYALTY");
        input.put("custname", "Test Store");
        input.put("ownername", "Test Owner");
        
        // Act
        Map<String, Object> result = transformer.transform(input);
        
        // Assert
        assertEquals("UID456", result.get("outletCode"));
        assertNull(result.get("storeid"));
    }
    
    @Test
    public void testNullOutletAddressNotMapped() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("outlet_address", null);
        input.put("uid", "UID456");
        input.put("type", "LOYALTY");
        input.put("custname", "Test Store");
        input.put("ownername", "Test Owner");
        
        // Act
        Map<String, Object> result = transformer.transform(input);
        
        // Assert
        assertNull(result.get("storeaddresss"));
    }
    
    @Test
    public void testEmptyOutletAddressNotMapped() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("outlet_address", "");
        input.put("uid", "UID456");
        input.put("type", "LOYALTY");
        input.put("custname", "Test Store");
        input.put("ownername", "Test Owner");
        
        // Act
        Map<String, Object> result = transformer.transform(input);
        
        // Assert
        assertNull(result.get("storeaddresss"));
    }
    
    @Test
    public void testBothFieldMappingsWork() {
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
        assertEquals("123 Main Street, City, State", result.get("storeaddresss"));
        assertNull(result.get("outletCode")); // Should not be set when outlet_id is present
    }
}