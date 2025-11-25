package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.component.model.LoadSequenceGenerator;
import com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException;
import com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType;
import com.salescode.dim.jooq.generated.tables.pojos.DmsVanLoadout;
import com.salescode.dim.jooq.impl.VanItems;
import com.salescode.dim.jooq.impl.VanLoadout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.salescode.dim.jooq.generated.tables.DmsVanLoadout.DMS_VAN_LOADOUT;


/**
 * Service for managing VanLoadout entities with hierarchical batch save operations.
 * This service orchestrates modular components for validation, hierarchy management,
 * and batch processing of VanLoadout and VanItems entities.
 */
public class VanLoadoutService extends AbstractCDMService<VanLoadout> {

    private static final Logger logger = LoggerFactory.getLogger(VanLoadoutService.class);
    public static final String VAN_LOADOUTS_COUNT = "vanLoadouts count=";

    private final VanLoadoutValidationService validationService;
    private final VanLoadoutHierarchyService hierarchyService;
    private final VanLoadoutBatchProcessor vanLoadoutBatchProcessor;
    private final VanItemsBatchProcessor vanItemsBatchProcessor;
    
    public VanLoadoutService() {
        // Initialize modular components
        LoadSequenceGenerator sequenceGenerator = new LoadSequenceGenerator(getDslContext());
        this.validationService = new VanLoadoutValidationService();
        this.hierarchyService = new VanLoadoutHierarchyService(sequenceGenerator, validationService);
        this.vanLoadoutBatchProcessor = new VanLoadoutBatchProcessor(getDslContext(), hierarchyService);
        this.vanItemsBatchProcessor = new VanItemsBatchProcessor(getDslContext(), hierarchyService, validationService);
    }

    /**
     * Main batchSave orchestration method for hierarchical VanLoadout data.
     * Processes VanLoadouts → VanItems in a single transaction.
     * 
     * @param vanLoadoutCollection collection of VanLoadout entities to save
     * @return the saved van loadout collection
     * @throws LoadoutBatchSaveException if any error occurs during the batch save operation
     *
     */
    @Override
    public Collection<VanLoadout> batchSave(Collection<VanLoadout> vanLoadoutCollection) throws LoadoutBatchSaveException {
        if (vanLoadoutCollection == null || vanLoadoutCollection.isEmpty()) {
            logger.info("Empty van loadout collection provided, returning empty result");
            return new ArrayList<>();
        }

        long startTime = System.currentTimeMillis();
        logger.info("Starting batch save operation for {} van loadouts", vanLoadoutCollection.size());

        try {
            executeBatchSaveTransaction(vanLoadoutCollection, startTime);
            return vanLoadoutCollection;
        } catch (LoadoutBatchSaveException e) {
            handleBatchSaveError(startTime, e);
            throw e;
        } catch (Exception e) {
            throw createFatalException(vanLoadoutCollection, startTime, e);
        }
    }

    /**
     * Executes the batch save operation within a transaction.
     * 
     * @param vanLoadoutCollection collection of van loadouts to save
     * @param startTime operation start time for logging
     * @throws LoadoutBatchSaveException if any phase fails
     */
    private void executeBatchSaveTransaction(Collection<VanLoadout> vanLoadoutCollection, long startTime) 
            throws LoadoutBatchSaveException {
        getDslContext().transaction(configuration -> {
            try {
                executePhase1PrepareHierarchy(vanLoadoutCollection);
                executePhase2ProcessVanLoadouts(vanLoadoutCollection);
                executePhase3ProcessVanItems(vanLoadoutCollection);
                logSuccessfulCompletion(vanLoadoutCollection, startTime);
            } catch (LoadoutBatchSaveException e) {
                // Just rethrow - logging happens at the boundary in handleBatchSaveError
                throw e;
            } catch (IllegalArgumentException e) {
                throw createValidationException(vanLoadoutCollection, e);
            } catch (Exception e) {
                throw createTransactionException(vanLoadoutCollection, e);
            }
        });
    }

    /**
     * Phase 1: Prepares entity hierarchy with IDs and references.
     * 
     * @param vanLoadoutCollection collection of van loadouts to prepare
     * @throws LoadoutBatchSaveException if hierarchy preparation fails
     */
    private void executePhase1PrepareHierarchy(Collection<VanLoadout> vanLoadoutCollection) 
            throws LoadoutBatchSaveException {
        logger.debug("Phase 1: Preparing entity hierarchy for {} van loadouts", vanLoadoutCollection.size());
        long phaseStart = System.currentTimeMillis();
        
        for (VanLoadout vanLoadout : vanLoadoutCollection) {
            prepareVanLoadoutHierarchy(vanLoadout);
        }
        
        logger.debug("Phase 1 completed in {}ms", System.currentTimeMillis() - phaseStart);
    }

    /**
     * Prepares a single van loadout's hierarchy.
     * 
     * @param vanLoadout the van loadout to prepare
     * @throws LoadoutBatchSaveException if preparation fails
     */
    private void prepareVanLoadoutHierarchy(VanLoadout vanLoadout) throws LoadoutBatchSaveException {
        try {
            hierarchyService.prepareEntityHierarchy(vanLoadout);
        } catch (IllegalArgumentException e) {
            throw createHierarchyValidationException(vanLoadout, e);
        } catch (Exception e) {
            throw createHierarchyPreparationException(vanLoadout, e);
        }
    }

    /**
     * Phase 2: Processes VanLoadout entities (root level).
     * 
     * @param vanLoadoutCollection collection of van loadouts to process
     * @throws LoadoutBatchSaveException if processing fails
     */
    private void executePhase2ProcessVanLoadouts(Collection<VanLoadout> vanLoadoutCollection) 
            throws LoadoutBatchSaveException {
        logger.debug("Phase 2: Processing {} VanLoadout entities", vanLoadoutCollection.size());
        long phaseStart = System.currentTimeMillis();
        
        try {
            vanLoadoutBatchProcessor.batchUpsertVanLoadouts(vanLoadoutCollection, this);
            logger.debug("Phase 2 completed in {}ms", System.currentTimeMillis() - phaseStart);
        } catch (LoadoutBatchSaveException e) {
            // Just rethrow - exception already contains context
            throw e;
        } catch (Exception e) {
            throw createPhaseException("VanLoadout", vanLoadoutCollection.size(), e);
        }
    }

    /**
     * Phase 3: Processes VanItems entities (child level).
     * 
     * @param vanLoadoutCollection collection of van loadouts containing items
     * @throws LoadoutBatchSaveException if processing fails
     */
    private void executePhase3ProcessVanItems(Collection<VanLoadout> vanLoadoutCollection) 
            throws LoadoutBatchSaveException {
        List<VanItems> allVanItems = hierarchyService.extractAllVanItems(vanLoadoutCollection);
        logger.debug("Phase 3: Processing {} VanItems entities", allVanItems.size());
        
        if (allVanItems.isEmpty()) {
            logger.debug("Phase 3 skipped - no VanItems to process");
            return;
        }
        
        long phaseStart = System.currentTimeMillis();
        try {
            vanItemsBatchProcessor.batchUpsertVanItems(allVanItems);
            logger.debug("Phase 3 completed in {}ms", System.currentTimeMillis() - phaseStart);
        } catch (IllegalArgumentException e) {
            throw createItemsValidationException(allVanItems, e);
        } catch (LoadoutBatchSaveException e) {
            // Just rethrow - exception already contains context
            throw e;
        } catch (Exception e) {
            throw createPhaseException("VanItems", allVanItems.size(), e);
        }
    }

    /**
     * Logs successful completion of batch save operation.
     */
    private void logSuccessfulCompletion(Collection<VanLoadout> vanLoadoutCollection, long startTime) {
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Successfully completed batch save operation for {} van loadouts in {}ms (avg {}ms per van loadout)", 
                   vanLoadoutCollection.size(), totalTime, totalTime / vanLoadoutCollection.size());
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
    private LoadoutBatchSaveException createHierarchyValidationException(VanLoadout vanLoadout, IllegalArgumentException e) {
        String loadNumber = getLoadNumber(vanLoadout);
        return new LoadoutBatchSaveException(
            "Failed to prepare van loadout hierarchy for loadNumber=" + loadNumber + ": " + e.getMessage(),
            ErrorType.VALIDATION_ERROR,
            "loadNumber=" + loadNumber,
            e
        );
    }

    /**
     * Creates exception for hierarchy preparation errors.
     */
    private LoadoutBatchSaveException createHierarchyPreparationException(VanLoadout vanLoadout, Exception e) {
        String loadNumber = getLoadNumber(vanLoadout);
        return new LoadoutBatchSaveException(
            "Failed to prepare van loadout hierarchy for loadNumber=" + loadNumber + ": " + e.getMessage(),
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
     * Creates exception for VanItems validation errors.
     */
    private LoadoutBatchSaveException createItemsValidationException(List<VanItems> items, IllegalArgumentException e) {
        return new LoadoutBatchSaveException(
            "Validation failed for VanItems: " + e.getMessage(),
            ErrorType.VALIDATION_ERROR,
            "phase=VanItems upsert, count=" + items.size(),
            e
        );
    }

    /**
     * Creates exception for validation errors during transaction.
     */
    private LoadoutBatchSaveException createValidationException(Collection<VanLoadout> vanLoadoutCollection, IllegalArgumentException e) {
        return new LoadoutBatchSaveException(
            "Validation error during batch save: " + e.getMessage(), 
            ErrorType.VALIDATION_ERROR, 
            VAN_LOADOUTS_COUNT + vanLoadoutCollection.size(), 
            e
        );
    }

    /**
     * Creates exception for transaction errors.
     */
    private LoadoutBatchSaveException createTransactionException(Collection<VanLoadout> vanLoadoutCollection, Exception e) {
        return new LoadoutBatchSaveException(
            "Batch save operation failed: " + e.getMessage(),
            ErrorType.TRANSACTION_ERROR,
            VAN_LOADOUTS_COUNT + vanLoadoutCollection.size(),
            e
        );
    }

    /**
     * Creates exception for fatal errors.
     * Logging is done here as this is the final boundary before throwing to caller.
     */
    private LoadoutBatchSaveException createFatalException(Collection<VanLoadout> vanLoadoutCollection, long startTime, Exception e) {
        long totalTime = System.currentTimeMillis() - startTime;
        logger.error("Fatal error in batch save operation after {}ms", totalTime, e);
        return new LoadoutBatchSaveException(
            "Batch save operation failed with error: " + e.getMessage(),
            ErrorType.TRANSACTION_ERROR,
            VAN_LOADOUTS_COUNT + vanLoadoutCollection.size() + ", duration=" + totalTime + "ms",
            e
        );
    }

    /**
     * Safely extracts loadNumber from a van loadout.
     */
    private String getLoadNumber(VanLoadout vanLoadout) {
        return vanLoadout.getDmsVanLoadout() != null ? vanLoadout.getDmsVanLoadout().getLoadNumber() : "unknown";
    }

    public DmsVanLoadout getExistingVanLoadout(String supplier, LocalDateTime activityDate, String routeCode, String salesmanId){
        try{
            return getDslContext()
                    .selectFrom(DMS_VAN_LOADOUT)
                    .where(DMS_VAN_LOADOUT.SUPPLIER.eq(supplier))
                    .and(DMS_VAN_LOADOUT.DELIVERY_START_DATE.eq(activityDate))
                    .and(DMS_VAN_LOADOUT.ROUTE_CODE.eq(Collections.singletonList(routeCode)))
                    .and(DMS_VAN_LOADOUT.SALESMAN_ID.eq(salesmanId))
                    .fetchOneInto(DmsVanLoadout.class);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }


    }
}
