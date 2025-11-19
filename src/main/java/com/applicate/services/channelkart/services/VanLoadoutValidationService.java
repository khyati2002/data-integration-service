package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.impl.VanItems;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Service responsible for validation of VanLoadout entities and their children.
 * This service provides validation logic for required fields and business rules
 * across the van loadout entity hierarchy.
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Validate required fields for VanItems (loadNumber, skuCode)</li>
 *   <li>Validate collections of VanItems</li>
 *   <li>Provide detailed error messages for validation failures</li>
 * </ul>
 * 
 * @see VanItems
 */
public class VanLoadoutValidationService {

    private static final Logger logger = LoggerFactory.getLogger(VanLoadoutValidationService.class);

    /**
     * Validates required fields for a single VanItems entity.
     * Checks that loadNumber and skuCode are not null or empty.
     * 
     * @param item the VanItems entity to validate
     * @throws IllegalArgumentException if validation fails with detailed error message
     *
     */
    public void validateVanItemsRequiredFields(VanItems item) {
        List<String> errors = new ArrayList<>();
        
        if (StringUtils.isBlank(item.getLoadNumber())) {
            errors.add("loadNumber cannot be null or empty");
        }
        if (StringUtils.isBlank(item.getSkuCode())) {
            errors.add("skuCode cannot be null or empty");
        }
        
        if (!errors.isEmpty()) {
            String errorMessage = "VanItems validation failed: " + String.join(", ", errors);
            logger.error("Validation error for VanItems: {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Validates all VanItems in a collection.
     * Validates each item and provides the index in error messages for easy identification.
     * 
     * @param items collection of VanItems entities to validate
     * @throws IllegalArgumentException if any item fails validation, with index information
     *
     */
    public void validateVanItemsCollection(Collection<VanItems> items) {
        if (items == null) {
            logger.debug("No VanItems to validate (null collection)");
            return;
        }
        
        logger.debug("Validating {} VanItems for required fields", items.size());
        
        int itemIndex = 0;
        for (VanItems item : items) {
            try {
                validateVanItemsRequiredFields(item);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Validation failed for VanItems at index " + itemIndex + ": " + e.getMessage(), e);
            }
            itemIndex++;
        }
        
        logger.debug("Successfully validated {} VanItems", items.size());
    }
}
