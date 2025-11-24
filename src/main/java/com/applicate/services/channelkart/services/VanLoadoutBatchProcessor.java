package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException;
import com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.salescode.dim.jooq.generated.enums.DmsVanLoadoutActiveStatus;
import com.salescode.dim.jooq.generated.tables.pojos.DmsVanLoadout;
import com.salescode.dim.jooq.generated.tables.records.DmsVanLoadoutRecord;
import com.salescode.dim.jooq.impl.VanLoadout;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.tables.DmsVanLoadout.DMS_VAN_LOADOUT;

/**
 * Processor responsible for batch operations on VanLoadout entities.
 * This class handles batch insert and update operations for VanLoadout entities,
 * including querying existing records, separating new from existing entities,
 * and executing JOOQ batch operations.
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Query existing van loadouts by loadNumber</li>
 *   <li>Batch insert new van loadout records</li>
 *   <li>Batch update existing van loadout records with version increment</li>
 *   <li>Manage common attributes and audit fields</li>
 * </ul>
 * 
 * @see VanLoadoutHierarchyService
 * @see AbstractCDMService
 */
public class VanLoadoutBatchProcessor {

    private static final Logger logger = LoggerFactory.getLogger(VanLoadoutBatchProcessor.class);
    private static final int INITIAL_VERSION = 0;
    
    private final DSLContext dslContext;
    private final VanLoadoutHierarchyService hierarchyService;
    
    public VanLoadoutBatchProcessor(DSLContext dslContext, VanLoadoutHierarchyService hierarchyService) {
        this.dslContext = dslContext;
        this.hierarchyService = hierarchyService;
    }

    /**
     * Queries existing van loadouts by loadNumber using JOOQ.
     * This method retrieves existing VanLoadout records from the database to determine
     * which van loadouts need to be inserted vs updated.
     * 
     * @param loadNumbers collection of loadNumbers to query for
     * @return map of loadNumber to VanLoadout entity for all found records
     * @throws LoadoutBatchSaveException if database query fails
     * 
     * <p>Requirements: 2.1, 2.2, 2.3</p>
     */
    public Map<String, VanLoadout> queryExistingVanLoadoutsByLoadNumber(Collection<String> loadNumbers) throws LoadoutBatchSaveException {
        if (loadNumbers == null || loadNumbers.isEmpty()) {
            logger.debug("No loadNumbers provided for query, returning empty map");
            return new HashMap<>();
        }

        logger.debug("Querying existing van loadouts for {} loadNumbers", loadNumbers.size());
        
        try {
            // Using proper JOOQ DSL with table constants
            List<com.salescode.dim.jooq.generated.tables.pojos.DmsVanLoadout> existingPojos =
                dslContext
                    .selectFrom(DMS_VAN_LOADOUT)
                    .where(DMS_VAN_LOADOUT.LOAD_NUMBER.in(loadNumbers))
                    .fetchInto(com.salescode.dim.jooq.generated.tables.pojos.DmsVanLoadout.class);

            Map<String, VanLoadout> existingVanLoadouts = new HashMap<>();
            for (com.salescode.dim.jooq.generated.tables.pojos.DmsVanLoadout pojo : existingPojos) {
                VanLoadout vanLoadout = new VanLoadout(pojo, null);
                existingVanLoadouts.put(pojo.getLoadNumber(), vanLoadout);
            }

            logger.debug("Found {} existing van loadouts out of {} requested loadNumbers", existingVanLoadouts.size(), loadNumbers.size());
            return existingVanLoadouts;

        } catch (org.jooq.exception.DataAccessException e) {
            throw new LoadoutBatchSaveException(
                "Database query failed for existing van loadouts: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "loadNumbers count=" + loadNumbers.size(),
                e
            );
        } catch (Exception e) {
            throw new LoadoutBatchSaveException(
                "Failed to query existing van loadouts: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "loadNumbers count=" + loadNumbers.size(),
                e
            );
        }
    }

    /**
     * Performs batch insert operation for new VanLoadout entities.
     * This method fills common attributes, sets initial version, and executes
     * a JOOQ batch insert for all new van loadout records.
     * 
     * @param newVanLoadouts collection of new VanLoadout entities to insert
     * @param cdmService service for filling common attributes
     * @throws LoadoutBatchSaveException if batch insert fails
     * 
     * <p>Requirements: 2.3, 5.1, 5.2, 5.3, 5.6, 7.1, 7.2</p>
     */
    public void batchInsertVanLoadouts(Collection<VanLoadout> newVanLoadouts, AbstractCDMService<VanLoadout> cdmService) throws LoadoutBatchSaveException {
        if (newVanLoadouts == null || newVanLoadouts.isEmpty()) {
            logger.debug("No new van loadouts to insert, skipping batch insert");
            return;
        }

        logger.debug("Preparing to batch insert {} new van loadouts", newVanLoadouts.size());
        
        try {
            // Fill common attributes for new entities
            for (VanLoadout vanLoadout : newVanLoadouts) {
                cdmService.fillCommonAttributes(vanLoadout);
                if (vanLoadout.getVersion() == null) {
                    vanLoadout.setVersion(INITIAL_VERSION);
                }
                vanLoadout.setOperationPerformed(ActionType.INSERT);
            }

            // Convert to JOOQ records for batch insert using proper JOOQ DSL
            List<com.salescode.dim.jooq.generated.tables.records.DmsVanLoadoutRecord> records = 
                newVanLoadouts.stream()
                    .map(vanLoadout -> {
                        DmsVanLoadoutRecord vanLoadoutRecord =
                            dslContext.newRecord(DMS_VAN_LOADOUT);
                        
                        // Map ALL fields from DmsVanLoadout POJO
                        DmsVanLoadout dmsVanLoadout = vanLoadout.getDmsVanLoadout();
                        if (dmsVanLoadout != null) {
                            // Set loadNumber as the primary key (id)
                            vanLoadoutRecord.setId(dmsVanLoadout.getLoadNumber());
                            vanLoadoutRecord.setLoadNumber(dmsVanLoadout.getLoadNumber());
                        }
                        
                        // Map common fields from the vanLoadout entity to the JOOQ vanLoadoutRecord
                        vanLoadoutRecord.setVersion(vanLoadout.getVersion());
                        vanLoadoutRecord.setActiveStatus(dmsVanLoadout.getActiveStatus());
                        vanLoadoutRecord.setCreationTime(vanLoadout.getCreationTime());
                        vanLoadoutRecord.setLastModifiedTime(vanLoadout.getLastModifiedTime());
                        vanLoadoutRecord.setCreatedBy(vanLoadout.getCreatedBy());
                        vanLoadoutRecord.setModifiedBy(vanLoadout.getModifiedBy());
                        
                        // Map ALL DMS-specific fields from DmsVanLoadout POJO
                        if (dmsVanLoadout != null) {
                            vanLoadoutRecord.setSupplier(dmsVanLoadout.getSupplier());
                            vanLoadoutRecord.setSalesmanId(dmsVanLoadout.getSalesmanId());
                            vanLoadoutRecord.setVehicleId(dmsVanLoadout.getVehicleId());
                            vanLoadoutRecord.setRouteCode(dmsVanLoadout.getRouteCode());
                            vanLoadoutRecord.setLoadOutStatus(dmsVanLoadout.getLoadOutStatus());
                            vanLoadoutRecord.setDeliveryStartDate(dmsVanLoadout.getDeliveryStartDate());
                            vanLoadoutRecord.setDeliveryEndDate(dmsVanLoadout.getDeliveryEndDate());
                            vanLoadoutRecord.setTotalAmount(dmsVanLoadout.getTotalAmount());
                            vanLoadoutRecord.setTotalCaseQty(dmsVanLoadout.getTotalCaseQty());
                            vanLoadoutRecord.setTotalPieceQty(dmsVanLoadout.getTotalPieceQty());
                            vanLoadoutRecord.setTotalOtherQty(dmsVanLoadout.getTotalOtherQty());
                            vanLoadoutRecord.setTotalCaseLeftQty(dmsVanLoadout.getTotalCaseLeftQty());
                            vanLoadoutRecord.setTotalPieceLeftQty(dmsVanLoadout.getTotalPieceLeftQty());
                            vanLoadoutRecord.setTotalOtherLeftQty(dmsVanLoadout.getTotalOtherLeftQty());
                            vanLoadoutRecord.setTotalSuggestedPieceQty(dmsVanLoadout.getTotalSuggestedPieceQty());
                            vanLoadoutRecord.setTotalSuggestedCaseQty(dmsVanLoadout.getTotalSuggestedCaseQty());
                            vanLoadoutRecord.setTotalSuggestedOtherQty(dmsVanLoadout.getTotalSuggestedOtherQty());
                            vanLoadoutRecord.setTotalAcceptedPieceQty(dmsVanLoadout.getTotalAcceptedPieceQty());
                            vanLoadoutRecord.setTotalAcceptedCaseQty(dmsVanLoadout.getTotalAcceptedCaseQty());
                            vanLoadoutRecord.setTotalAcceptedOtherQty(dmsVanLoadout.getTotalAcceptedOtherQty());
                        }
                        
                        return vanLoadoutRecord;
                    })
                    .collect(Collectors.toList());

            // Execute batch insert using JOOQ batch API
            int[] results = dslContext.batchInsert(records).execute();
            
            int successCount = 0;
            for (int result : results) {
                if (result > 0) successCount++;
            }

            logger.info("Successfully batch inserted {} new van loadouts ({} records affected)", newVanLoadouts.size(), successCount);

        } catch (org.jooq.exception.DataAccessException e) {
            throw new LoadoutBatchSaveException(
                "Batch insert failed for new van loadouts due to database error: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "new van loadouts count=" + newVanLoadouts.size(),
                e
            );
        } catch (Exception e) {
            throw new LoadoutBatchSaveException(
                "Batch insert failed for new van loadouts: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "new van loadouts count=" + newVanLoadouts.size(),
                e
            );
        }
    }

    /**
     * Performs batch update operation for existing VanLoadout entities with version increment.
     * This method fills common attributes, increments version numbers, and executes
     * a JOOQ batch update for all existing van loadout records.
     * 
     * @param existingVanLoadouts collection of existing VanLoadout entities to update
     * @param cdmService service for filling common attributes
     * @throws LoadoutBatchSaveException if batch update fails
     * 
     * <p>Requirements: 2.2, 5.1, 5.2, 5.4, 5.6, 7.1, 7.2</p>
     */
    public void batchUpdateVanLoadouts(Collection<VanLoadout> existingVanLoadouts, AbstractCDMService<VanLoadout> cdmService) throws LoadoutBatchSaveException {
        if (existingVanLoadouts == null || existingVanLoadouts.isEmpty()) {
            logger.debug("No existing van loadouts to update, skipping batch update");
            return;
        }

        logger.debug("Preparing to batch update {} existing van loadouts", existingVanLoadouts.size());
        
        try {
            // Fill common attributes and increment version for existing entities
            for (VanLoadout vanLoadout : existingVanLoadouts) {
                cdmService.fillCommonAttributes(vanLoadout);
                // Increment version for existing records
                Integer currentVersion = vanLoadout.getVersion();
                vanLoadout.setVersion(currentVersion != null ? currentVersion + 1 : INITIAL_VERSION);
                vanLoadout.setOperationPerformed(ActionType.UPDATE);
            }

            // Convert to JOOQ update queries using proper JOOQ DSL
            List<org.jooq.Query> updateQueries = existingVanLoadouts.stream()
                .map(vanLoadout -> {
                    DmsVanLoadout dmsVanLoadout = vanLoadout.getDmsVanLoadout();
                    
                    return dslContext
                        .update(DMS_VAN_LOADOUT)
                        // Update common fields
                        .set(DMS_VAN_LOADOUT.VERSION, vanLoadout.getVersion())
                        .set(DMS_VAN_LOADOUT.ACTIVE_STATUS, dmsVanLoadout.getActiveStatus())
                        .set(DMS_VAN_LOADOUT.LAST_MODIFIED_TIME, vanLoadout.getLastModifiedTime())
                        .set(DMS_VAN_LOADOUT.MODIFIED_BY, vanLoadout.getModifiedBy())
                        // Update ALL DMS-specific fields from DmsVanLoadout POJO
                        .set(DMS_VAN_LOADOUT.SUPPLIER, dmsVanLoadout.getSupplier())
                        .set(DMS_VAN_LOADOUT.SALESMAN_ID, dmsVanLoadout.getSalesmanId())
                        .set(DMS_VAN_LOADOUT.VEHICLE_ID, dmsVanLoadout.getVehicleId())
                        .set(DMS_VAN_LOADOUT.ROUTE_CODE, dmsVanLoadout.getRouteCode())
                        .set(DMS_VAN_LOADOUT.LOAD_OUT_STATUS, dmsVanLoadout.getLoadOutStatus())
                        .set(DMS_VAN_LOADOUT.DELIVERY_START_DATE, dmsVanLoadout.getDeliveryStartDate())
                        .set(DMS_VAN_LOADOUT.DELIVERY_END_DATE, dmsVanLoadout.getDeliveryEndDate())
                        .set(DMS_VAN_LOADOUT.TOTAL_AMOUNT, dmsVanLoadout.getTotalAmount())
                        .set(DMS_VAN_LOADOUT.TOTAL_CASE_QTY, dmsVanLoadout.getTotalCaseQty())
                        .set(DMS_VAN_LOADOUT.TOTAL_PIECE_QTY, dmsVanLoadout.getTotalPieceQty())
                        .set(DMS_VAN_LOADOUT.TOTAL_OTHER_QTY, dmsVanLoadout.getTotalOtherQty())
                        .set(DMS_VAN_LOADOUT.TOTAL_CASE_LEFT_QTY, dmsVanLoadout.getTotalCaseLeftQty())
                        .set(DMS_VAN_LOADOUT.TOTAL_PIECE_LEFT_QTY, dmsVanLoadout.getTotalPieceLeftQty())
                        .set(DMS_VAN_LOADOUT.TOTAL_OTHER_LEFT_QTY, dmsVanLoadout.getTotalOtherLeftQty())
                        .set(DMS_VAN_LOADOUT.TOTAL_SUGGESTED_PIECE_QTY, dmsVanLoadout.getTotalSuggestedPieceQty())
                        .set(DMS_VAN_LOADOUT.TOTAL_SUGGESTED_CASE_QTY, dmsVanLoadout.getTotalSuggestedCaseQty())
                        .set(DMS_VAN_LOADOUT.TOTAL_SUGGESTED_OTHER_QTY, dmsVanLoadout.getTotalSuggestedOtherQty())
                        .set(DMS_VAN_LOADOUT.TOTAL_ACCEPTED_PIECE_QTY, dmsVanLoadout.getTotalAcceptedPieceQty())
                        .set(DMS_VAN_LOADOUT.TOTAL_ACCEPTED_CASE_QTY, dmsVanLoadout.getTotalAcceptedCaseQty())
                        .set(DMS_VAN_LOADOUT.TOTAL_ACCEPTED_OTHER_QTY, dmsVanLoadout.getTotalAcceptedOtherQty())
                        .where(DMS_VAN_LOADOUT.ID.eq(dmsVanLoadout.getLoadNumber())); // Using ID since loadNumber is the primary key
                })
                .collect(Collectors.toList());

            // Execute batch update using JOOQ batch API
            int[] results = dslContext.batch(updateQueries).execute();
            
            int successCount = 0;
            for (int result : results) {
                if (result > 0) successCount++;
            }

            logger.info("Successfully batch updated {} existing van loadouts ({} records affected)", existingVanLoadouts.size(), successCount);

        } catch (org.jooq.exception.DataAccessException e) {
            throw new LoadoutBatchSaveException(
                "Batch update failed for existing van loadouts due to database error: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "existing van loadouts count=" + existingVanLoadouts.size(),
                e
            );
        } catch (Exception e) {
            throw new LoadoutBatchSaveException(
                "Batch update failed for existing van loadouts: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "existing van loadouts count=" + existingVanLoadouts.size(),
                e
            );
        }
    }

    /**
     * Main method to perform batch upsert operations for VanLoadout entities.
     * This method orchestrates the complete upsert process: ensuring loadNumbers exist,
     * querying for existing records, separating new from existing, and executing
     * batch insert and update operations.
     * 
     * @param vanLoadouts collection of VanLoadout entities to upsert
     * @param cdmService service for filling common attributes
     * @throws LoadoutBatchSaveException if any operation fails
     * 
     * <p>Requirements: 2.1, 2.2, 2.3, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 7.1, 7.2</p>
     */
    public void batchUpsertVanLoadouts(Collection<VanLoadout> vanLoadouts, AbstractCDMService<VanLoadout> cdmService) {
        if (vanLoadouts == null || vanLoadouts.isEmpty()) {
            return;
        }

        try {
            // Ensure all van loadouts have loadNumbers
            hierarchyService.ensureLoadNumbers(vanLoadouts);

            // Set loadNumber as ID for all van loadouts (since loadNumber is the primary key)
            for (VanLoadout vanLoadout : vanLoadouts) {
                if (vanLoadout.getDmsVanLoadout() != null && vanLoadout.getDmsVanLoadout().getLoadNumber() != null) {
                    vanLoadout.setId(vanLoadout.getDmsVanLoadout().getLoadNumber());
                }
            }

            // Extract loadNumbers for querying existing records
            Set<String> loadNumbers = vanLoadouts.stream()
                .map(vanLoadout -> vanLoadout.getDmsVanLoadout().getLoadNumber())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            // Query existing van loadouts by loadNumber
            Map<String, VanLoadout> existingVanLoadoutsMap = queryExistingVanLoadoutsByLoadNumber(loadNumbers);

            // Separate into new and existing van loadouts
            List<VanLoadout> newVanLoadouts = new ArrayList<>();
            List<VanLoadout> existingVanLoadouts = new ArrayList<>();

            for (VanLoadout vanLoadout : vanLoadouts) {
                String loadNumber = vanLoadout.getDmsVanLoadout().getLoadNumber();
                if (existingVanLoadoutsMap.containsKey(loadNumber)) {
                    // Update existing van loadout with new data while preserving database fields
                    VanLoadout existingVanLoadout = existingVanLoadoutsMap.get(loadNumber);
                    AbstractCDMService.fillAttributes(vanLoadout, existingVanLoadout); // Copy non-null fields from input to existing
                    existingVanLoadouts.add(vanLoadout);
                } else {
                    newVanLoadouts.add(vanLoadout);
                }
            }

            // Perform batch operations
            batchInsertVanLoadouts(newVanLoadouts, cdmService);
            batchUpdateVanLoadouts(existingVanLoadouts, cdmService);

            logger.info("Completed batch upsert for {} van loadouts ({} new, {} updated)", 
                       vanLoadouts.size(), newVanLoadouts.size(), existingVanLoadouts.size());

        } catch (LoadoutBatchSaveException e) {
            // Just rethrow - exception already contains context
            throw e;
        } catch (Exception e) {
            throw new LoadoutBatchSaveException(
                "Batch upsert failed for van loadouts: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "van loadouts count=" + vanLoadouts.size(),
                e
            );
        }
    }
}
