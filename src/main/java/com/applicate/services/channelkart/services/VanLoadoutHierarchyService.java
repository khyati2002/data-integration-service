package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.component.model.LoadSequenceGenerator;
import com.salescode.dim.jooq.impl.VanLoadout;
import com.salescode.dim.jooq.impl.VanItems;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for ID generation and hierarchy preparation for VanLoadout entities.
 * This service manages the generation of unique identifiers and ensures proper
 * parent-child relationships across the two-level hierarchy:
 * VanLoadout → VanItems.
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Generate loadNumbers using sequence service</li>
 *   <li>Create composite keys for VanItems uniqueness checks</li>
 *   <li>Set parent references to maintain referential integrity</li>
 *   <li>Prepare entire entity hierarchy with proper IDs and references</li>
 * </ul>
 * 
 * @see LoadSequenceGenerator
 * @see VanLoadoutValidationService
 */
public class VanLoadoutHierarchyService {

    private static final Logger logger = LoggerFactory.getLogger(VanLoadoutHierarchyService.class);
    private static final String RSU_LOAD_NUMBER_SEQUENCE_NAME = "rsuLoadNumber";
    
    private final LoadSequenceGenerator sequenceGenerator;
    private final VanLoadoutValidationService validationService;
    
    public VanLoadoutHierarchyService(LoadSequenceGenerator sequenceGenerator, VanLoadoutValidationService validationService) {
        if (sequenceGenerator == null) {
            throw new IllegalArgumentException("LoadSequenceGenerator cannot be null");
        }
        if (validationService == null) {
            throw new IllegalArgumentException("VanLoadoutValidationService cannot be null");
        }
        this.sequenceGenerator = sequenceGenerator;
        this.validationService = validationService;
    }

    /**
     * Generates loadNumber using the database routine getNextLoadVal.
     * Calls the LoadSequenceGenerator to get the next value for the rsuLoadNumber sequence.
     * 
     * @return the generated loadNumber as a string
     * @throws com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException if sequence generation fails or returns null
     * 
     * <p>Requirements: 4.1, 4.2, 4.3</p>
     */
    public String generateLoadNumber() {
        logger.debug("Generating new loadNumber using getNextLoadVal routine with sequence name: {}", RSU_LOAD_NUMBER_SEQUENCE_NAME);
        
        try {
            String generatedLoadNumber = sequenceGenerator.getGeneratedSequenceNumber(RSU_LOAD_NUMBER_SEQUENCE_NAME, "VL", null);
            logger.debug("Generated loadNumber: {}", generatedLoadNumber);
            return generatedLoadNumber;
            
        } catch (com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException e) {
            // Re-throw our custom exception with context
            throw e;
        } catch (Exception e) {
            throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                "Unable to generate loadNumber from database routine: " + e.getMessage(),
                com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.SEQUENCE_GENERATION_ERROR,
                "entity=VanLoadout, field=loadNumber, sequenceName=" + RSU_LOAD_NUMBER_SEQUENCE_NAME,
                e
            );
        }
    }

    /**
     * Ensures all van loadouts have loadNumbers, generating them if missing.
     * Iterates through all van loadouts and generates loadNumbers for any that don't have one.
     * Also sets the loadNumber as the ID since it's used as the primary key identifier.
     * 
     * @param vanLoadouts collection of VanLoadout entities to process
     * @throws com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException if loadNumber generation fails
     * @throws IllegalArgumentException if a van loadout has null DmsVanLoadout object
     * 
     * <p>Requirements: 4.1, 4.2, 4.3</p>
     */
    public void ensureLoadNumbers(Collection<VanLoadout> vanLoadouts) {
        logger.debug("Ensuring all {} van loadouts have loadNumbers", vanLoadouts.size());
        
        int generatedCount = 0;
        for (VanLoadout vanLoadout : vanLoadouts) {
            if (vanLoadout.getDmsVanLoadout() == null) {
                logger.error("VanLoadout has null DmsVanLoadout object, cannot process");
                throw new IllegalArgumentException("VanLoadout must have a non-null DmsVanLoadout object");
            }
            
            if (StringUtils.isBlank(vanLoadout.getDmsVanLoadout().getLoadNumber())) {
                try {
                    String generatedLoadNumber = generateLoadNumber();
                    vanLoadout.getDmsVanLoadout().setLoadNumber(generatedLoadNumber);
                    // Set loadNumber as ID since it's the primary key
                    vanLoadout.setId(generatedLoadNumber);
                    logger.debug("Generated and assigned loadNumber {} for van loadout", generatedLoadNumber);
                    generatedCount++;
                } catch (com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException e) {
                    // Just rethrow - exception already contains context
                    throw e;
                } catch (Exception e) {
                    throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                        "Failed to ensure loadNumber for van loadout: " + e.getMessage(),
                        com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.SEQUENCE_GENERATION_ERROR,
                        "van loadout processing",
                        e
                    );
                }
            }
        }
        
        if (generatedCount > 0) {
            logger.info("Generated {} new loadNumbers out of {} total van loadouts", generatedCount, vanLoadouts.size());
        } else {
            logger.debug("All van loadouts already have loadNumbers, no generation needed");
        }
    }

    /**
     * Creates composite key string for VanItems uniqueness check.
     * Uses pipe-delimited format: loadNumber|skuCode|batchCode|batchId|itemType.
     * Handles null values by converting them to empty strings.
     * 
     * @param item the VanItems entity to create key for
     * @return composite key string for uniqueness checking
     * 
     * <p>Requirements: 6.3, 6.4</p>
     */
    public String createVanItemsCompositeKey(VanItems item) {
        return String.join("|", 
            StringUtils.defaultString(item.getLoadNumber()),
            StringUtils.defaultString(item.getSkuCode()),
            StringUtils.defaultString(item.getBatchCode()),
            StringUtils.defaultString(item.getBatchId()),
            item.getItemType() != null ? item.getItemType().getLiteral() : ""
        );
    }

    /**
     * Sets parent references for VanItems to link them to VanLoadout.
     * Sets the loadNumber field for each VanItems entity.
     * 
     * @param items collection of VanItems to set parent references for
     * @param loadNumber the parent VanLoadout loadNumber to set
     * 
     * <p>Requirements: 6.1, 6.2, 6.3, 6.4</p>
     */
    public void setVanItemsParentReferences(Collection<VanItems> items, String loadNumber) {
        if (items == null) {
            logger.debug("No VanItems to set parent references for (null collection)");
            return;
        }
        
        logger.debug("Setting parent loadNumber {} for {} VanItems", loadNumber, items.size());
        
        for (VanItems item : items) {
            item.setLoadNumber(loadNumber);
        }
        
        logger.trace("Successfully set parent references for {} VanItems", items.size());
    }

    /**
     * Prepares all entities in the hierarchy with proper IDs and references.
     * This is the main orchestration method that ensures the entire two-level hierarchy
     * (VanLoadout → VanItems) has proper IDs and parent-child references.
     * 
     * @param vanLoadout the root VanLoadout entity to prepare
     * @throws IllegalArgumentException if validation fails or required fields are missing
     * @throws com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException if loadNumber generation fails
     * 
     * <p>Requirements: 4.1, 4.2, 4.3, 6.1, 6.2, 6.3, 6.4</p>
     */
    public void prepareEntityHierarchy(VanLoadout vanLoadout) {
        logger.trace("Preparing entity hierarchy for van loadout");
        
        // Validate van loadout structure
        if (vanLoadout.getDmsVanLoadout() == null) {
            logger.error("VanLoadout has null DmsVanLoadout object");
            throw new IllegalArgumentException("VanLoadout must have a non-null DmsVanLoadout object");
        }
        
        // Ensure van loadout has loadNumber
        if (StringUtils.isBlank(vanLoadout.getDmsVanLoadout().getLoadNumber())) {
            try {
                String generatedLoadNumber = generateLoadNumber();
                vanLoadout.getDmsVanLoadout().setLoadNumber(generatedLoadNumber);
                vanLoadout.setId(generatedLoadNumber);
                logger.debug("Generated loadNumber {} for van loadout during hierarchy preparation", generatedLoadNumber);
            } catch (com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException e) {
                // Just rethrow - exception already contains context
                throw e;
            } catch (Exception e) {
                throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                    "Failed to prepare van loadout hierarchy: " + e.getMessage(),
                    com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.HIERARCHY_PREPARATION_ERROR,
                    "loadNumber generation",
                    e
                );
            }
        }
        
        String loadNumber = vanLoadout.getDmsVanLoadout().getLoadNumber();
        logger.trace("Processing van loadout hierarchy for loadNumber={}", loadNumber);
        
        // Process VanItems
        if (vanLoadout.getVanItemsList() != null && !vanLoadout.getVanItemsList().isEmpty()) {
            logger.debug("Processing {} VanItems for loadNumber={}", vanLoadout.getVanItemsList().size(), loadNumber);
            
            try {
                setVanItemsParentReferences(vanLoadout.getVanItemsList(), loadNumber);
                validationService.validateVanItemsCollection(vanLoadout.getVanItemsList());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Failed to prepare VanItems for loadNumber=" + loadNumber + ": " + e.getMessage(), e);
            }
        } else {
            logger.debug("No VanItems to process for loadNumber={}", loadNumber);
        }
        
        logger.trace("Successfully prepared entity hierarchy for loadNumber={}", loadNumber);
    }

    /**
     * Extracts all VanItems from a collection of VanLoadouts.
     * Flattens the hierarchy to get all VanItems across all VanLoadouts.
     * 
     * @param vanLoadouts collection of VanLoadout entities
     * @return list of all VanItems from all van loadouts
     * 
     * <p>Requirements: 6.3, 6.4</p>
     */
    public List<VanItems> extractAllVanItems(Collection<VanLoadout> vanLoadouts) {
        return vanLoadouts.stream()
            .filter(vanLoadout -> vanLoadout.getVanItemsList() != null)
            .flatMap(vanLoadout -> vanLoadout.getVanItemsList().stream())
            .collect(Collectors.toList());
    }
}
