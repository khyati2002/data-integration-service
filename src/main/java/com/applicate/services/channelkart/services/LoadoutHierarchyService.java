package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.component.model.LoadSequenceGenerator;
import com.salescode.dim.jooq.impl.Loadout;
import com.salescode.dim.jooq.impl.LoadoutDetails;
import com.salescode.dim.jooq.impl.LoadoutItems;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsible for ID generation and hierarchy preparation for Loadout entities.
 * This service manages the generation of unique identifiers and ensures proper
 * parent-child relationships across the three-level hierarchy:
 * Loadout → LoadoutDetails → LoadoutItems.
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Generate loadNumbers using sequence service</li>
 *   <li>Generate composite IDs for LoadoutDetails (loadNumber-invoiceNumber)</li>
 *   <li>Create composite keys for LoadoutItems uniqueness checks</li>
 *   <li>Set parent references to maintain referential integrity</li>
 *   <li>Prepare entire entity hierarchy with proper IDs and references</li>
 * </ul>
 * 
 * @see LoadSequenceGenerator
 * @see LoadoutValidationService
 */
public class LoadoutHierarchyService {

    private static final Logger logger = LoggerFactory.getLogger(LoadoutHierarchyService.class);
    private static final String LOADOUT_SEQUENCE_NAME = "loadNumber";
    
    private final LoadSequenceGenerator sequenceGenerator;
    private final LoadoutValidationService validationService;
    
    public LoadoutHierarchyService(LoadSequenceGenerator sequenceGenerator, LoadoutValidationService validationService) {
        if (sequenceGenerator == null) {
            throw new IllegalArgumentException("LoadSequenceGenerator cannot be null");
        }
        if (validationService == null) {
            throw new IllegalArgumentException("LoadoutValidationService cannot be null");
        }
        this.sequenceGenerator = sequenceGenerator;
        this.validationService = validationService;
    }

    /**
     * Generates loadNumber using the database routine getNextLoadVal.
     * Calls the LoadSequenceGenerator to get the next value for the loadNumber sequence.
     * 
     * @return the generated loadNumber as a string
     * @throws LoadoutBatchSaveException if sequence generation fails or returns null
     *
     */
    public String generateLoadNumber() {
        logger.debug("Generating new loadNumber using getNextLoadVal routine");
        
        try {
            String generatedLoadNumber = sequenceGenerator.getGeneratedSequenceNumber(LOADOUT_SEQUENCE_NAME, "LN", null);
            logger.debug("Generated loadNumber: {}", generatedLoadNumber);
            return generatedLoadNumber;
            
        } catch (com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException e) {
            // Re-throw our custom exception with context
            throw e;
        } catch (Exception e) {
            throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                "Unable to generate loadNumber from database routine: " + e.getMessage(),
                com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.SEQUENCE_GENERATION_ERROR,
                "entity=Loadout, field=loadNumber, sequenceName=" + LOADOUT_SEQUENCE_NAME,
                e
            );
        }
    }

    /**
     * Ensures all loadouts have loadNumbers, generating them if missing.
     * Iterates through all loadouts and generates loadNumbers for any that don't have one.
     * Also sets the loadNumber as the ID since it's the primary key.
     * 
     * @param loadouts collection of Loadout entities to process
     * @throws LoadoutBatchSaveException if loadNumber generation fails
     * @throws IllegalArgumentException if a loadout has null DmsLoadout object
     *
     */
    public void ensureLoadNumbers(Collection<Loadout> loadouts) {
        logger.debug("Ensuring all {} loadouts have loadNumbers", loadouts.size());
        
        int generatedCount = 0;
        for (Loadout loadout : loadouts) {
            if (loadout.getDmsLoadout() == null) {
                logger.error("Loadout has null DmsLoadout object, cannot process");
                throw new IllegalArgumentException("Loadout must have a non-null DmsLoadout object");
            }
            
            if (StringUtils.isBlank(loadout.getDmsLoadout().getLoadNumber())) {
                try {
                    String generatedLoadNumber = generateLoadNumber();
                    loadout.getDmsLoadout().setLoadNumber(generatedLoadNumber);
                    // Set loadNumber as ID since it's the primary key
                    loadout.setId(generatedLoadNumber);
                    logger.debug("Generated and assigned loadNumber {} for loadout", generatedLoadNumber);
                    generatedCount++;
                } catch (com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException e) {
                    // Just rethrow - exception already contains context
                    throw e;
                } catch (Exception e) {
                    throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                        "Failed to ensure loadNumber for loadout: " + e.getMessage(),
                        com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.SEQUENCE_GENERATION_ERROR,
                        "loadout processing",
                        e
                    );
                }
            }
        }
        
        if (generatedCount > 0) {
            logger.info("Generated {} new loadNumbers out of {} total loadouts", generatedCount, loadouts.size());
        } else {
            logger.debug("All loadouts already have loadNumbers, no generation needed");
        }
    }

    /**
     * Generates composite ID for LoadoutDetails using format "loadNumber-invoiceNumber".
     * 
     * @param loadNumber the loadNumber from the parent Loadout
     * @param invoiceNumber the invoiceNumber from the LoadoutDetails
     * @return composite ID in format "loadNumber-invoiceNumber"
     * @throws IllegalArgumentException if loadNumber or invoiceNumber is null or empty
     *
     */
    public String generateLoadoutDetailsId(String loadNumber, String invoiceNumber) {
        if (StringUtils.isBlank(loadNumber)) {
            logger.error("Attempted to generate LoadoutDetails ID with null or empty loadNumber");
            throw new IllegalArgumentException("loadNumber cannot be null or empty for LoadoutDetails ID generation");
        }
        if (StringUtils.isBlank(invoiceNumber)) {
            logger.error("Attempted to generate LoadoutDetails ID with null or empty invoiceNumber for loadNumber={}", loadNumber);
            throw new IllegalArgumentException("invoiceNumber cannot be null or empty for LoadoutDetails ID generation");
        }
        
        String compositeId = loadNumber + "-" + invoiceNumber;
        logger.trace("Generated LoadoutDetails composite ID: {}", compositeId);
        return compositeId;
    }

    /**
     * Generates composite IDs (loadNumber-invoiceNumber) for LoadoutDetails.
     * Sets the ID field for each LoadoutDetails entity using the composite format.
     * 
     * @param loadoutDetails collection of LoadoutDetails entities to process
     *
     */
    public void generateLoadoutDetailsCompositeIds(Collection<LoadoutDetails> loadoutDetails) {
        if (loadoutDetails == null || loadoutDetails.isEmpty()) {
            return;
        }

        for (LoadoutDetails detail : loadoutDetails) {
            if (StringUtils.isNotBlank(detail.getLoadNumber()) && StringUtils.isNotBlank(detail.getInvoiceNumber())) {
                String compositeId = detail.getLoadNumber() + "-" + detail.getInvoiceNumber();
                detail.setId(compositeId);
            }
        }
    }

    /**
     * Sets composite IDs for all LoadoutDetails entities.
     * Generates and sets the composite ID for each LoadoutDetails and ensures
     * the parent loadNumber reference is set.
     * 
     * @param details collection of LoadoutDetails entities to process
     * @param loadNumber the parent loadNumber to use for all details
     * @throws IllegalArgumentException if any LoadoutDetails has null or empty invoiceNumber
     *
     */
    public void setLoadoutDetailsIds(Collection<LoadoutDetails> details, String loadNumber) {
        if (details == null) {
            logger.debug("No LoadoutDetails to set IDs for (null collection)");
            return;
        }
        
        logger.debug("Setting composite IDs for {} LoadoutDetails with loadNumber={}", details.size(), loadNumber);
        
        int detailIndex = 0;
        for (LoadoutDetails detail : details) {
            try {
                if (StringUtils.isBlank(detail.getInvoiceNumber())) {
                    logger.error("LoadoutDetails at index {} has null or empty invoiceNumber for loadNumber={}", detailIndex, loadNumber);
                    throw new IllegalArgumentException("LoadoutDetails.invoiceNumber cannot be null or empty at index " + detailIndex);
                }
                
                String compositeId = generateLoadoutDetailsId(loadNumber, detail.getInvoiceNumber());
                detail.setId(compositeId);
                detail.setLoadNumber(loadNumber); // Ensure parent reference is set
                
            } catch (IllegalArgumentException e) {
                logger.error("Failed to set ID for LoadoutDetails at index {} with loadNumber={}", detailIndex, loadNumber, e);
                throw e;
            }
            detailIndex++;
        }
        
        logger.debug("Successfully set composite IDs for {} LoadoutDetails", details.size());
    }

    /**
     * Creates composite key string for LoadoutItems uniqueness check.
     * Uses pipe-delimited format: loadoutDetailsId|skuCode|batchCode|batchId|itemType.
     * Handles null values by converting them to empty strings.
     * 
     * @param item the LoadoutItems entity to create key for
     * @return composite key string for uniqueness checking
     *
     */
    public String createLoadoutItemsCompositeKey(LoadoutItems item) {
        return String.join("|", 
            StringUtils.defaultString(item.getLoadOutDetailsId()),
            StringUtils.defaultString(item.getSkuCode()),
            StringUtils.defaultString(item.getBatchCode()),
            StringUtils.defaultString(item.getBatchId()),
            item.getItemType() != null ? item.getItemType().getLiteral() : ""
        );
    }

    /**
     * Groups LoadoutItems by their composite key for uniqueness processing.
     * If duplicate keys are found, the latest item replaces the earlier one.
     * 
     * @param items collection of LoadoutItems to group
     * @return map of composite key to LoadoutItems entity
     *
     */
    public Map<String, LoadoutItems> groupLoadoutItemsByCompositeKey(Collection<LoadoutItems> items) {
        if (items == null) return Map.of();
        
        return items.stream()
            .collect(Collectors.toMap(
                this::createLoadoutItemsCompositeKey,
                item -> item,
                (existing, replacement) -> {
                    logger.warn("Duplicate LoadoutItems found with composite key: {}. Using latest.", 
                        createLoadoutItemsCompositeKey(existing));
                    return replacement;
                }
            ));
    }

    /**
     * Sets parent references for LoadoutItems to link them to LoadoutDetails.
     * Sets the loadOutDetailsId field for each LoadoutItems entity.
     * 
     * @param items collection of LoadoutItems to set parent references for
     * @param loadoutDetailsId the parent LoadoutDetails ID to set
     *
     */
    public void setLoadoutItemsParentReferences(Collection<LoadoutItems> items, String loadoutDetailsId) {
        if (items == null) return;
        
        for (LoadoutItems item : items) {
            item.setLoadOutDetailsId(loadoutDetailsId);
        }
    }

    /**
     * Prepares all entities in the hierarchy with proper IDs and references.
     * This is the main orchestration method that ensures the entire three-level hierarchy
     * (Loadout → LoadoutDetails → LoadoutItems) has proper IDs and parent-child references.
     * 
     * @param loadout the root Loadout entity to prepare
     * @throws IllegalArgumentException if validation fails or required fields are missing
     * @throws LoadoutBatchSaveException if loadNumber generation fails
     *
     */
    public void prepareEntityHierarchy(Loadout loadout) {
        logger.trace("Preparing entity hierarchy for loadout");
        
        // Validate loadout structure
        if (loadout.getDmsLoadout() == null) {
            logger.error("Loadout has null DmsLoadout object");
            throw new IllegalArgumentException("Loadout must have a non-null DmsLoadout object");
        }
        
        // Ensure loadout has loadNumber
        if (StringUtils.isBlank(loadout.getDmsLoadout().getLoadNumber())) {
            try {
                String generatedLoadNumber = generateLoadNumber();
                loadout.getDmsLoadout().setLoadNumber(generatedLoadNumber);
                logger.debug("Generated loadNumber {} for loadout during hierarchy preparation", generatedLoadNumber);
            } catch (com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException e) {
                // Just rethrow - exception already contains context
                throw e;
            } catch (Exception e) {
                throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                    "Failed to prepare loadout hierarchy: " + e.getMessage(),
                    com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.HIERARCHY_PREPARATION_ERROR,
                    "loadNumber generation",
                    e
                );
            }
        }
        
        String loadNumber = loadout.getDmsLoadout().getLoadNumber();
        logger.trace("Processing loadout hierarchy for loadNumber={}", loadNumber);
        
        // Process LoadoutDetails
        if (loadout.getLoadoutDetailsList() != null && !loadout.getLoadoutDetailsList().isEmpty()) {
            logger.debug("Processing {} LoadoutDetails for loadNumber={}", loadout.getLoadoutDetailsList().size(), loadNumber);
            
            try {
                setLoadoutDetailsIds(loadout.getLoadoutDetailsList(), loadNumber);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Failed to prepare LoadoutDetails for loadNumber=" + loadNumber + ": " + e.getMessage(), e);
            }
            
            // Process LoadoutItems for each LoadoutDetails
            int detailIndex = 0;
            for (LoadoutDetails detail : loadout.getLoadoutDetailsList()) {
                if (detail.getLoadoutItems() != null && !detail.getLoadoutItems().isEmpty()) {
                    logger.debug("Processing {} LoadoutItems for LoadoutDetails ID={}", detail.getLoadoutItems().size(), detail.getId());
                    
                    try {
                        setLoadoutItemsParentReferences(detail.getLoadoutItems(), detail.getId());
                        validationService.validateLoadoutItemsCollection(detail.getLoadoutItems());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Failed to prepare LoadoutItems for LoadoutDetails ID=" + detail.getId() + ": " + e.getMessage(), e);
                    }
                }
                detailIndex++;
            }
        } else {
            logger.debug("No LoadoutDetails to process for loadNumber={}", loadNumber);
        }
        
        logger.trace("Successfully prepared entity hierarchy for loadNumber={}", loadNumber);
    }

    /**
     * Extracts all LoadoutDetails from a collection of Loadouts.
     * Flattens the hierarchy to get all LoadoutDetails across all Loadouts.
     * 
     * @param loadouts collection of Loadout entities
     * @return list of all LoadoutDetails from all loadouts
     */
    public List<LoadoutDetails> extractAllLoadoutDetails(Collection<Loadout> loadouts) {
        return loadouts.stream()
            .filter(loadout -> loadout.getLoadoutDetailsList() != null)
            .flatMap(loadout -> loadout.getLoadoutDetailsList().stream())
            .collect(Collectors.toList());
    }

    /**
     * Extracts all LoadoutItems from a collection of LoadoutDetails.
     * Flattens the hierarchy to get all LoadoutItems across all LoadoutDetails.
     * 
     * @param details collection of LoadoutDetails entities
     * @return list of all LoadoutItems from all details
     */
    public List<LoadoutItems> extractAllLoadoutItems(Collection<LoadoutDetails> details) {
        return details.stream()
            .filter(detail -> detail.getLoadoutItems() != null)
            .flatMap(detail -> detail.getLoadoutItems().stream())
            .collect(Collectors.toList());
    }
}
