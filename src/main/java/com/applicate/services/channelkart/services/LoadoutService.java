package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException;
import com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType;
import com.salescode.dim.jooq.impl.Loadout;
import com.salescode.dim.jooq.impl.LoadoutDetails;
import com.salescode.dim.jooq.impl.LoadoutItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Service for managing Loadout entities with hierarchical batch save operations.
 * This service orchestrates modular components for validation, hierarchy management,
 * and batch processing of Loadout, LoadoutDetails, and LoadoutItems entities.
 */
public class LoadoutService extends AbstractCDMService<Loadout> {

    private static final Logger logger = LoggerFactory.getLogger(LoadoutService.class);
    public static final String LOADOUTS_COUNT = "loadouts count=";

    private final LoadoutValidationService validationService;
    private final LoadoutHierarchyService hierarchyService;
    private final LoadoutBatchProcessor loadoutBatchProcessor;
    private final LoadoutDetailsBatchProcessor loadoutDetailsBatchProcessor;
    private final LoadoutItemsBatchProcessor loadoutItemsBatchProcessor;
    
    public LoadoutService() {
        // Initialize modular components
        this.validationService = new LoadoutValidationService();
        this.hierarchyService = new LoadoutHierarchyService( validationService);
        this.loadoutBatchProcessor = new LoadoutBatchProcessor(getDslContext(), hierarchyService);
        this.loadoutDetailsBatchProcessor = new LoadoutDetailsBatchProcessor(getDslContext(), hierarchyService);
        this.loadoutItemsBatchProcessor = new LoadoutItemsBatchProcessor(getDslContext(), hierarchyService, validationService);
    }

    /**
     * Main batchSave orchestration method for hierarchical Loadout data.
     * Processes Loadouts → LoadoutDetails → LoadoutItems in a single transaction.
     * 
     * @param loadoutCollection collection of Loadout entities to save
     * @return the saved loadout collection
     * @throws LoadoutBatchSaveException if any error occurs during the batch save operation
     * 
     * <p>Requirements: 1.1, 1.2, 1.3, 6.4, 7.1, 7.2, 7.3, 7.4</p>
     */
    @Override
    public Collection<Loadout> batchSave(Collection<Loadout> loadoutCollection) throws LoadoutBatchSaveException {
        if (loadoutCollection == null || loadoutCollection.isEmpty()) {
            logger.info("Empty loadout collection provided, returning empty result");
            return new ArrayList<>();
        }

        long startTime = System.currentTimeMillis();
        logger.info("Starting batch save operation for {} loadouts", loadoutCollection.size());

        try {
            executeBatchSaveTransaction(loadoutCollection, startTime);
            return loadoutCollection;
        } catch (LoadoutBatchSaveException e) {
            handleBatchSaveError(startTime, e);
            throw e;
        } catch (Exception e) {
            throw createFatalException(loadoutCollection, startTime, e);
        }
    }

    /**
     * Executes the batch save operation within a transaction.
     * 
     * @param loadoutCollection collection of loadouts to save
     * @param startTime operation start time for logging
     * @throws LoadoutBatchSaveException if any phase fails
     */
    private void executeBatchSaveTransaction(Collection<Loadout> loadoutCollection, long startTime) 
            throws LoadoutBatchSaveException {
        getDslContext().transaction(configuration -> {
            try {
                executePhase1PrepareHierarchy(loadoutCollection);
                executePhase2ProcessLoadouts(loadoutCollection);
                executePhase3ProcessLoadoutDetails(loadoutCollection);
                executePhase4ProcessLoadoutItems(loadoutCollection);
                logSuccessfulCompletion(loadoutCollection, startTime);
            } catch (LoadoutBatchSaveException e) {
                // Just rethrow - logging happens at the boundary in handleBatchSaveError
                throw e;
            } catch (IllegalArgumentException e) {
                throw createValidationException(loadoutCollection, e);
            } catch (Exception e) {
                throw createTransactionException(loadoutCollection, e);
            }
        });
    }

    /**
     * Phase 1: Prepares entity hierarchy with IDs and references.
     * 
     * @param loadoutCollection collection of loadouts to prepare
     * @throws LoadoutBatchSaveException if hierarchy preparation fails
     */
    private void executePhase1PrepareHierarchy(Collection<Loadout> loadoutCollection) 
            throws LoadoutBatchSaveException {
        logger.debug("Phase 1: Preparing entity hierarchy for {} loadouts", loadoutCollection.size());
        long phaseStart = System.currentTimeMillis();
        
        for (Loadout loadout : loadoutCollection) {
            prepareLoadoutHierarchy(loadout);
        }
        
        logger.debug("Phase 1 completed in {}ms", System.currentTimeMillis() - phaseStart);
    }

    /**
     * Prepares a single loadout's hierarchy.
     * 
     * @param loadout the loadout to prepare
     * @throws LoadoutBatchSaveException if preparation fails
     */
    private void prepareLoadoutHierarchy(Loadout loadout) throws LoadoutBatchSaveException {
        try {
            hierarchyService.prepareEntityHierarchy(loadout);
        } catch (IllegalArgumentException e) {
            throw createHierarchyValidationException(loadout, e);
        } catch (Exception e) {
            throw createHierarchyPreparationException(loadout, e);
        }
    }

    /**
     * Phase 2: Processes Loadout entities (root level).
     * 
     * @param loadoutCollection collection of loadouts to process
     * @throws LoadoutBatchSaveException if processing fails
     */
    private void executePhase2ProcessLoadouts(Collection<Loadout> loadoutCollection) 
            throws LoadoutBatchSaveException {
        logger.debug("Phase 2: Processing {} Loadout entities", loadoutCollection.size());
        long phaseStart = System.currentTimeMillis();
        
        try {
            loadoutBatchProcessor.batchUpsertLoadouts(loadoutCollection, this);
            logger.debug("Phase 2 completed in {}ms", System.currentTimeMillis() - phaseStart);
        } catch (LoadoutBatchSaveException e) {
            // Just rethrow - exception already contains context
            throw e;
        } catch (Exception e) {
            throw createPhaseException("Loadout", loadoutCollection.size(), e);
        }
    }

    /**
     * Phase 3: Processes LoadoutDetails entities (child level).
     * 
     * @param loadoutCollection collection of loadouts containing details
     * @throws LoadoutBatchSaveException if processing fails
     */
    private void executePhase3ProcessLoadoutDetails(Collection<Loadout> loadoutCollection) 
            throws LoadoutBatchSaveException {
        List<LoadoutDetails> allLoadoutDetails = hierarchyService.extractAllLoadoutDetails(loadoutCollection);
        logger.debug("Phase 3: Processing {} LoadoutDetails entities", allLoadoutDetails.size());
        
        if (allLoadoutDetails.isEmpty()) {
            logger.debug("Phase 3 skipped - no LoadoutDetails to process");
            return;
        }
        
        long phaseStart = System.currentTimeMillis();
        try {
            loadoutDetailsBatchProcessor.batchUpsertLoadoutDetails(allLoadoutDetails);
            logger.debug("Phase 3 completed in {}ms", System.currentTimeMillis() - phaseStart);
        } catch (LoadoutBatchSaveException e) {
            // Just rethrow - exception already contains context
            throw e;
        } catch (Exception e) {
            throw createPhaseException("LoadoutDetails", allLoadoutDetails.size(), e);
        }
    }

    /**
     * Phase 4: Processes LoadoutItems entities (grandchild level).
     * 
     * @param loadoutCollection collection of loadouts containing items
     * @throws LoadoutBatchSaveException if processing fails
     */
    private void executePhase4ProcessLoadoutItems(Collection<Loadout> loadoutCollection) 
            throws LoadoutBatchSaveException {
        List<LoadoutDetails> allLoadoutDetails = hierarchyService.extractAllLoadoutDetails(loadoutCollection);
        List<LoadoutItems> allLoadoutItems = hierarchyService.extractAllLoadoutItems(allLoadoutDetails);
        logger.debug("Phase 4: Processing {} LoadoutItems entities", allLoadoutItems.size());
        
        if (allLoadoutItems.isEmpty()) {
            logger.debug("Phase 4 skipped - no LoadoutItems to process");
            return;
        }
        
        long phaseStart = System.currentTimeMillis();
        try {
            loadoutItemsBatchProcessor.batchUpsertLoadoutItems(allLoadoutItems);
            logger.debug("Phase 4 completed in {}ms", System.currentTimeMillis() - phaseStart);
        } catch (IllegalArgumentException e) {
            throw createItemsValidationException(allLoadoutItems, e);
        } catch (LoadoutBatchSaveException e) {
            // Just rethrow - exception already contains context
            throw e;
        } catch (Exception e) {
            throw createPhaseException("LoadoutItems", allLoadoutItems.size(), e);
        }
    }

    /**
     * Logs successful completion of batch save operation.
     */
    private void logSuccessfulCompletion(Collection<Loadout> loadoutCollection, long startTime) {
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Successfully completed batch save operation for {} loadouts in {}ms (avg {}ms per loadout)", 
                   loadoutCollection.size(), totalTime, totalTime / loadoutCollection.size());
    }

    /**
     * Handles batch save error logging.
     */
    private void handleBatchSaveError(long startTime, LoadoutBatchSaveException e) {
        long totalTime = System.currentTimeMillis() - startTime;
        logger.error("Batch save operation failed after {}ms", totalTime, e);
    }

    /**
     * Creates exception for hierarchy validation errors.
     * Logging is done here as this is where we create and throw the exception.
     */
    private LoadoutBatchSaveException createHierarchyValidationException(Loadout loadout, IllegalArgumentException e) {
        String loadNumber = getLoadNumber(loadout);
        return new LoadoutBatchSaveException(
            "Failed to prepare loadout hierarchy for loadNumber=" + loadNumber + ": " + e.getMessage(),
            ErrorType.VALIDATION_ERROR,
            "loadNumber=" + loadNumber,
            e
        );
    }

    /**
     * Creates exception for hierarchy preparation errors.
     */
    private LoadoutBatchSaveException createHierarchyPreparationException(Loadout loadout, Exception e) {
        String loadNumber = getLoadNumber(loadout);
        return new LoadoutBatchSaveException(
            "Failed to prepare loadout hierarchy for loadNumber=" + loadNumber + ": " + e.getMessage(),
            ErrorType.HIERARCHY_PREPARATION_ERROR,
            "loadNumber=" + loadNumber,
            e
        );
    }

    /**
     * Creates exception for phase processing errors.
     */
    private LoadoutBatchSaveException createPhaseException(String entityType, int count, Exception e) {
        return new LoadoutBatchSaveException(
            "Failed to process " + entityType + " entities: " + e.getMessage(),
            ErrorType.DATABASE_ERROR,
            "phase=" + entityType + " upsert, count=" + count,
            e
        );
    }

    /**
     * Creates exception for LoadoutItems validation errors.
     */
    private LoadoutBatchSaveException createItemsValidationException(List<LoadoutItems> items, IllegalArgumentException e) {
        return new LoadoutBatchSaveException(
            "Validation failed for LoadoutItems: " + e.getMessage(),
            ErrorType.VALIDATION_ERROR,
            "phase=LoadoutItems upsert, count=" + items.size(),
            e
        );
    }

    /**
     * Creates exception for validation errors during transaction.
     */
    private LoadoutBatchSaveException createValidationException(Collection<Loadout> loadoutCollection, IllegalArgumentException e) {
        return new LoadoutBatchSaveException(
            "Validation error during batch save: " + e.getMessage(), ErrorType.VALIDATION_ERROR, LOADOUTS_COUNT + loadoutCollection.size(), e
        );
    }

    /**
     * Creates exception for transaction errors.
     */
    private LoadoutBatchSaveException createTransactionException(Collection<Loadout> loadoutCollection, Exception e) {
        return new LoadoutBatchSaveException(
            "Batch save operation failed: " + e.getMessage(),
            ErrorType.TRANSACTION_ERROR,
            LOADOUTS_COUNT + loadoutCollection.size(),
            e
        );
    }

    /**
     * Creates exception for fatal errors.
     * Logging is done here as this is the final boundary before throwing to caller.
     */
    private LoadoutBatchSaveException createFatalException(Collection<Loadout> loadoutCollection, long startTime, Exception e) {
        long totalTime = System.currentTimeMillis() - startTime;
        logger.error("Fatal error in batch save operation after {}ms", totalTime, e);
        return new LoadoutBatchSaveException(
            "Batch save operation failed with error: " + e.getMessage(),
            ErrorType.TRANSACTION_ERROR,
            LOADOUTS_COUNT + loadoutCollection.size() + ", duration=" + totalTime + "ms",
            e
        );
    }

    /**
     * Safely extracts loadNumber from a loadout.
     */
    private String getLoadNumber(Loadout loadout) {
        return loadout.getDmsLoadout() != null ? loadout.getDmsLoadout().getLoadNumber() : "unknown";
    }
}
