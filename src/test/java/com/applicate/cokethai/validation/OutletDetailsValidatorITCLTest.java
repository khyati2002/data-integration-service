package com.applicate.cokethai.validation;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.etl.OperationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OutletDetailsValidatorITCLTest {
    
    private OutletDetailsValidatorITCL validator;
    private CommonDataModel mockCdm;
    private ObjectMapper objectMapper;
    
    @Before
    public void setUp() {
        validator = new OutletDetailsValidatorITCL();
        mockCdm = mock(CommonDataModel.class);
        objectMapper = new ObjectMapper();
    }
    
    @Test
    public void testValidationPassesWithValidStoreid() {
        // Arrange
        ObjectNode extendedAttributes = objectMapper.createObjectNode();
        extendedAttributes.put("storeid", "STORE123");
        
        when(mockCdm.getExtendedAttributes()).thenReturn(extendedAttributes);
        when(mockCdm.getId()).thenReturn("test-id");
        
        // Act
        OperationResult.StepResult result = validator.apply(mockCdm);
        
        // Assert
        assertEquals(OperationResult.StepResult.OK, result);
    }
    
    @Test
    public void testValidationPassesWithValidStoreaddresss() {
        // Arrange
        ObjectNode extendedAttributes = objectMapper.createObjectNode();
        extendedAttributes.put("storeaddresss", "123 Main Street, City, State");
        
        when(mockCdm.getExtendedAttributes()).thenReturn(extendedAttributes);
        when(mockCdm.getId()).thenReturn("test-id");
        
        // Act
        OperationResult.StepResult result = validator.apply(mockCdm);
        
        // Assert
        assertEquals(OperationResult.StepResult.OK, result);
    }
    
    @Test
    public void testValidationFailsWithEmptyStoreid() {
        // Arrange
        ObjectNode extendedAttributes = objectMapper.createObjectNode();
        extendedAttributes.put("storeid", "");
        
        when(mockCdm.getExtendedAttributes()).thenReturn(extendedAttributes);
        when(mockCdm.getId()).thenReturn("test-id");
        
        // Act
        OperationResult.StepResult result = validator.apply(mockCdm);
        
        // Assert
        assertNotEquals(OperationResult.StepResult.OK, result);
        assertTrue(result.getMessage().contains("storeid field cannot be empty"));
    }
    
    @Test
    public void testValidationFailsWithInvalidStoreidCharacters() {
        // Arrange
        ObjectNode extendedAttributes = objectMapper.createObjectNode();
        extendedAttributes.put("storeid", "STORE@123#");
        
        when(mockCdm.getExtendedAttributes()).thenReturn(extendedAttributes);
        when(mockCdm.getId()).thenReturn("test-id");
        
        // Act
        OperationResult.StepResult result = validator.apply(mockCdm);
        
        // Assert
        assertNotEquals(OperationResult.StepResult.OK, result);
        assertTrue(result.getMessage().contains("storeid field contains invalid characters"));
    }
    
    @Test
    public void testValidationFailsWithEmptyStoreaddresss() {
        // Arrange
        ObjectNode extendedAttributes = objectMapper.createObjectNode();
        extendedAttributes.put("storeaddresss", "");
        
        when(mockCdm.getExtendedAttributes()).thenReturn(extendedAttributes);
        when(mockCdm.getId()).thenReturn("test-id");
        
        // Act
        OperationResult.StepResult result = validator.apply(mockCdm);
        
        // Assert
        assertNotEquals(OperationResult.StepResult.OK, result);
        assertTrue(result.getMessage().contains("storeaddresss field cannot be empty"));
    }
    
    @Test
    public void testValidationFailsWithTooShortAddress() {
        // Arrange
        ObjectNode extendedAttributes = objectMapper.createObjectNode();
        extendedAttributes.put("storeaddresss", "123");
        
        when(mockCdm.getExtendedAttributes()).thenReturn(extendedAttributes);
        when(mockCdm.getId()).thenReturn("test-id");
        
        // Act
        OperationResult.StepResult result = validator.apply(mockCdm);
        
        // Assert
        assertNotEquals(OperationResult.StepResult.OK, result);
        assertTrue(result.getMessage().contains("storeaddresss field must be between 5 and 500 characters"));
    }
    
    @Test
    public void testValidationFailsWithTooLongAddress() {
        // Arrange
        ObjectNode extendedAttributes = objectMapper.createObjectNode();
        String longAddress = "A".repeat(501);
        extendedAttributes.put("storeaddresss", longAddress);
        
        when(mockCdm.getExtendedAttributes()).thenReturn(extendedAttributes);
        when(mockCdm.getId()).thenReturn("test-id");
        
        // Act
        OperationResult.StepResult result = validator.apply(mockCdm);
        
        // Assert
        assertNotEquals(OperationResult.StepResult.OK, result);
        assertTrue(result.getMessage().contains("storeaddresss field must be between 5 and 500 characters"));
    }
    
    @Test
    public void testValidationPassesWithNullExtendedAttributes() {
        // Arrange
        when(mockCdm.getExtendedAttributes()).thenReturn(null);
        when(mockCdm.getId()).thenReturn("test-id");
        
        // Act
        OperationResult.StepResult result = validator.apply(mockCdm);
        
        // Assert
        assertEquals(OperationResult.StepResult.OK, result);
    }
    
    @Test
    public void testValidationPassesWithBothValidFields() {
        // Arrange
        ObjectNode extendedAttributes = objectMapper.createObjectNode();
        extendedAttributes.put("storeid", "STORE123");
        extendedAttributes.put("storeaddresss", "123 Main Street, City, State");
        
        when(mockCdm.getExtendedAttributes()).thenReturn(extendedAttributes);
        when(mockCdm.getId()).thenReturn("test-id");
        
        // Act
        OperationResult.StepResult result = validator.apply(mockCdm);
        
        // Assert
        assertEquals(OperationResult.StepResult.OK, result);
    }
}