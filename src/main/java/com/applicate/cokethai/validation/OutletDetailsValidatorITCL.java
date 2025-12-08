package com.applicate.cokethai.validation;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutletDetailsValidatorITCL extends AbstractValidationRule<CommonDataModel> {
    private static final Logger logger = LoggerFactory.getLogger(OutletDetailsValidatorITCL.class);
    
    @Override
    public OperationResult.StepResult apply(CommonDataModel cdm) {
        try {
            // Validate field mappings for outlet_address -> storeaddresss and outlet_id -> storeid
            JsonNode extendedAttributes = cdm.getExtendedAttributes();
            
            if (extendedAttributes != null) {
                // Validate storeid field mapping
                JsonNode storeIdNode = extendedAttributes.get("storeid");
                if (storeIdNode != null) {
                    String storeId = storeIdNode.asText();
                    if (storeId == null || storeId.trim().isEmpty()) {
                        logger.error("Validation failed: storeid field is empty or null for entity ID: {}", cdm.getId());
                        return OperationResult.StepResult.FAILED("Validation failed: storeid field cannot be empty");
                    }
                    
                    // Validate storeid format (basic validation - should be alphanumeric)
                    if (!storeId.matches("^[a-zA-Z0-9_-]+$")) {
                        logger.error("Validation failed: storeid field contains invalid characters for entity ID: {}", cdm.getId());
                        return OperationResult.StepResult.FAILED("Validation failed: storeid field contains invalid characters");
                    }
                }
                
                // Validate storeaddresss field mapping
                JsonNode storeAddressNode = extendedAttributes.get("storeaddresss");
                if (storeAddressNode != null) {
                    String storeAddress = storeAddressNode.asText();
                    if (storeAddress == null || storeAddress.trim().isEmpty()) {
                        logger.error("Validation failed: storeaddresss field is empty or null for entity ID: {}", cdm.getId());
                        return OperationResult.StepResult.FAILED("Validation failed: storeaddresss field cannot be empty");
                    }
                    
                    // Validate address length (business rule: address should be between 5 and 500 characters)
                    if (storeAddress.length() < 5 || storeAddress.length() > 500) {
                        logger.error("Validation failed: storeaddresss field length is invalid for entity ID: {}", cdm.getId());
                        return OperationResult.StepResult.FAILED("Validation failed: storeaddresss field must be between 5 and 500 characters");
                    }
                }
            }
            
            logger.debug("Outlet details validation passed for entity ID: {}", cdm.getId());
            return OperationResult.StepResult.OK;
            
        } catch (Exception e) {
            logger.error("Validation error occurred for entity ID: {}", cdm.getId(), e);
            return OperationResult.StepResult.FAILED("Validation error: " + e.getMessage());
        }
    }
}