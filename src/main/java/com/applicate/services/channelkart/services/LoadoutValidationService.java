package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.impl.LoadoutItems;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Service responsible for validation of Loadout entities and their children.
 * This service provides validation logic for required fields and business rules
 * across the loadout entity hierarchy.
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Validate required fields for LoadoutItems (loadoutDetailsId, skuCode)</li>
 *   <li>Validate collections of LoadoutItems</li>
 *   <li>Provide detailed error messages for validation failures</li>
 * </ul>
 * 
 * @see LoadoutItems
 */
public class LoadoutValidationService {

    private static final Logger logger = LoggerFactory.getLogger(LoadoutValidationService.class);

    /**
     * Validates required fields for a single LoadoutItems entity.
     * Checks that loadOutDetailsId and skuCode are not null or empty.
     * 
     * @param item the LoadoutItems entity to validate
     * @throws IllegalArgumentException if validation fails with detailed error message
     * 
     * <p>Requirements: 4.1, 4.5</p>
     */
    public void validateLoadoutItemsRequiredFields(LoadoutItems item) {
        List<String> errors = new ArrayList<>();
        
        if (StringUtils.isBlank(item.getLoadOutDetailsId())) {
            errors.add("loadOutDetailsId cannot be null or empty");
        }
        if (StringUtils.isBlank(item.getSkuCode())) {
            errors.add("skuCode cannot be null or empty");
        }
        
        if (!errors.isEmpty()) {
            String errorMessage = "LoadoutItems validation failed: " + String.join(", ", errors);
            logger.error("Validation error for LoadoutItems: {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Validates all LoadoutItems in a collection.
     * Validates each item and provides the index in error messages for easy identification.
     * 
     * @param items collection of LoadoutItems entities to validate
     * @throws IllegalArgumentException if any item fails validation, with index information
     * 
     * <p>Requirements: 4.1, 4.5</p>
     */
    public void validateLoadoutItemsCollection(Collection<LoadoutItems> items) {
        if (items == null) {
            logger.debug("No LoadoutItems to validate (null collection)");
            return;
        }
        
        logger.debug("Validating {} LoadoutItems for required fields", items.size());
        
        int itemIndex = 0;
        for (LoadoutItems item : items) {
            try {
                validateLoadoutItemsRequiredFields(item);
            } catch (IllegalArgumentException e) {
                logger.error("Validation failed for LoadoutItems at index {}: {}", itemIndex, e.getMessage());
                throw new IllegalArgumentException("Validation failed for LoadoutItems at index " + itemIndex + ": " + e.getMessage(), e);
            }
            itemIndex++;
        }
        
        logger.debug("Successfully validated {} LoadoutItems", items.size());
    }
}
