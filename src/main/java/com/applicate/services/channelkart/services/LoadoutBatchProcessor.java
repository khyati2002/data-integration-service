package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException;
import com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.jooq.generated.enums.DmsLoadoutActiveStatus;
import com.salescode.dim.jooq.generated.tables.pojos.DmsLoadout;
import com.salescode.dim.jooq.generated.tables.records.DmsLoadoutRecord;
import com.salescode.dim.jooq.impl.Loadout;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.tables.DmsLoadout.DMS_LOADOUT;

/**
 * Processor responsible for batch operations on Loadout entities.
 * This class handles batch insert and update operations for Loadout entities,
 * including querying existing records, separating new from existing entities,
 * and executing JOOQ batch operations.
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Query existing loadouts by loadNumber</li>
 *   <li>Batch insert new loadout records</li>
 *   <li>Batch update existing loadout records with version increment</li>
 *   <li>Manage common attributes and audit fields</li>
 * </ul>
 * 
 * @see LoadoutHierarchyService
 * @see AbstractCDMService
 */
public class LoadoutBatchProcessor {

    private static final Logger logger = LoggerFactory.getLogger(LoadoutBatchProcessor.class);
    private static final int INITIAL_VERSION = 0;
    
    private final DSLContext dslContext;
    private final LoadoutHierarchyService hierarchyService;
    
    public LoadoutBatchProcessor(DSLContext dslContext, LoadoutHierarchyService hierarchyService) {
        this.dslContext = dslContext;
        this.hierarchyService = hierarchyService;
    }

    /**
     * Queries existing loadouts by loadNumber using JOOQ.
     * This method retrieves existing Loadout records from the database to determine
     * which loadouts need to be inserted vs updated.
     * 
     * @param loadNumbers collection of loadNumbers to query for
     * @return map of loadNumber to Loadout entity for all found records
     * @throws LoadoutBatchSaveException if database query fails
     * 
     * <p>Requirements: 2.1, 6.1, 6.2</p>
     */
    public Map<String, Loadout> queryExistingLoadoutsByLoadNumber(Collection<String> loadNumbers) throws LoadoutBatchSaveException {
        if (loadNumbers == null || loadNumbers.isEmpty()) {
            logger.debug("No loadNumbers provided for query, returning empty map");
            return new HashMap<>();
        }

        logger.debug("Querying existing loadouts for {} loadNumbers", loadNumbers.size());
        
        try {
            // Using proper JOOQ DSL with table constants
            List<com.salescode.dim.jooq.generated.tables.pojos.DmsLoadout> existingPojos = 
                dslContext
                    .selectFrom(DMS_LOADOUT)
                    .where(DMS_LOADOUT.LOAD_NUMBER.in(loadNumbers))
                    .fetchInto(com.salescode.dim.jooq.generated.tables.pojos.DmsLoadout.class);

            Map<String, Loadout> existingLoadouts = new HashMap<>();
            for (com.salescode.dim.jooq.generated.tables.pojos.DmsLoadout pojo : existingPojos) {
                Loadout loadout = new Loadout();
                loadout.setDmsLoadout(pojo);
                existingLoadouts.put(pojo.getLoadNumber(), loadout);
            }

            logger.debug("Found {} existing loadouts out of {} requested loadNumbers", existingLoadouts.size(), loadNumbers.size());
            return existingLoadouts;

        } catch (org.jooq.exception.DataAccessException e) {
            throw new LoadoutBatchSaveException(
                "Database query failed for existing loadouts: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "loadNumbers count=" + loadNumbers.size(),
                e
            );
        } catch (Exception e) {
            throw new LoadoutBatchSaveException(
                "Failed to query existing loadouts: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "loadNumbers count=" + loadNumbers.size(),
                e
            );
        }
    }

    /**
     * Performs batch insert operation for new Loadout entities.
     * This method fills common attributes, sets initial version, and executes
     * a JOOQ batch insert for all new loadout records.
     * 
     * @param newLoadouts collection of new Loadout entities to insert
     * @param cdmService service for filling common attributes
     * @throws LoadoutBatchSaveException if batch insert fails
     * 
     * <p>Requirements: 2.3, 6.3, 8.1, 8.2</p>
     */
    public void batchInsertLoadouts(Collection<Loadout> newLoadouts, AbstractCDMService<Loadout> cdmService) throws LoadoutBatchSaveException {
        if (newLoadouts == null || newLoadouts.isEmpty()) {
            logger.debug("No new loadouts to insert, skipping batch insert");
            return;
        }

        logger.debug("Preparing to batch insert {} new loadouts", newLoadouts.size());
        
        try {
            // Fill common attributes for new entities
            for (Loadout loadout : newLoadouts) {
                cdmService.fillCommonAttributes(loadout);
                if (loadout.getVersion() == null) {
                    loadout.setVersion(INITIAL_VERSION);
                }
                loadout.setOperationPerformed(ActionType.INSERT);
            }

            // Convert to JOOQ records for batch insert using proper JOOQ DSL
            List<com.salescode.dim.jooq.generated.tables.records.DmsLoadoutRecord> records = 
                newLoadouts.stream()
                    .map(loadout -> {
                        DmsLoadoutRecord dmsLoadoutRecord =
                            dslContext.newRecord(DMS_LOADOUT);
                        
                        // Map ALL DMS-specific fields from DmsLoadout POJO
                        DmsLoadout dmsLoadout = loadout.getDmsLoadout();
                        if (dmsLoadout != null) {
                            // Set loadNumber as the primary key (id)
                            dmsLoadoutRecord.setId(dmsLoadout.getLoadNumber());
                            dmsLoadoutRecord.setLoadNumber(dmsLoadout.getLoadNumber());
                        }
                        
                        // Map common fields from the loadout entity to the JOOQ dmsLoadoutRecord
                        dmsLoadoutRecord.setVersion(loadout.getVersion());
                        dmsLoadoutRecord.setActiveStatus(dmsLoadout.getActiveStatus());
                        dmsLoadoutRecord.setCreationTime(loadout.getCreationTime());
                        dmsLoadoutRecord.setLastModifiedTime(loadout.getLastModifiedTime());
                        dmsLoadoutRecord.setCreatedBy(loadout.getCreatedBy());
                        dmsLoadoutRecord.setModifiedBy(loadout.getModifiedBy());
                        
                        // Continue mapping DMS-specific fields
                        if (dmsLoadout != null) {
                            dmsLoadoutRecord.setLoadOutStatus(dmsLoadout.getLoadOutStatus());
                            dmsLoadoutRecord.setLoadOutType(dmsLoadout.getLoadOutType());
                            dmsLoadoutRecord.setSalesmanId(dmsLoadout.getSalesmanId());
                            dmsLoadoutRecord.setSupplier(dmsLoadout.getSupplier());
                            dmsLoadoutRecord.setTotalAmount(dmsLoadout.getTotalAmount());
                            dmsLoadoutRecord.setTotalCaseLeftQty(dmsLoadout.getTotalCaseLeftQty());
                            dmsLoadoutRecord.setTotalCaseQty(dmsLoadout.getTotalCaseQty());
                            dmsLoadoutRecord.setTotalOtherLeftQty(dmsLoadout.getTotalOtherLeftQty());
                            dmsLoadoutRecord.setTotalOtherQty(dmsLoadout.getTotalOtherQty());
                            dmsLoadoutRecord.setTotalPieceLeftQty(dmsLoadout.getTotalPieceLeftQty());
                            dmsLoadoutRecord.setTotalPieceQty(dmsLoadout.getTotalPieceQty());
                            dmsLoadoutRecord.setVehicleCapacity(dmsLoadout.getVehicleCapacity());
                            dmsLoadoutRecord.setVehicleId(dmsLoadout.getVehicleId());
                            dmsLoadoutRecord.setLoadOutDate(dmsLoadout.getLoadOutDate());
                            dmsLoadoutRecord.setLoadoutSource(dmsLoadout.getLoadoutSource());
                            dmsLoadoutRecord.setCaseShortage(dmsLoadout.getCaseShortage());
                            dmsLoadoutRecord.setOtherShortage(dmsLoadout.getOtherShortage());
                            dmsLoadoutRecord.setPieceShortage(dmsLoadout.getPieceShortage());
                            dmsLoadoutRecord.setDeliveryStartDate(dmsLoadout.getDeliveryStartDate());
                            dmsLoadoutRecord.setDeliveryEndDate(dmsLoadout.getDeliveryEndDate());
                            dmsLoadoutRecord.setSettlementDate(dmsLoadout.getSettlementDate());
                            dmsLoadoutRecord.setShortageUpdated(dmsLoadout.getShortageUpdated());
                            dmsLoadoutRecord.setInvoiceCreationStartDate(dmsLoadout.getInvoiceCreationStartDate());
                            dmsLoadoutRecord.setInvoiceCreationEndDate(dmsLoadout.getInvoiceCreationEndDate());
                        }
                        
                        return dmsLoadoutRecord;
                    })
                    .collect(Collectors.toList());

            // Execute batch insert using JOOQ batch API
            int[] results = dslContext.batchInsert(records).execute();
            
            int successCount = 0;
            for (int result : results) {
                if (result > 0) successCount++;
            }

            logger.info("Successfully batch inserted {} new loadouts ({} records affected)", newLoadouts.size(), successCount);

        } catch (org.jooq.exception.DataAccessException e) {
            throw new LoadoutBatchSaveException(
                "Batch insert failed for new loadouts due to database error: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "new loadouts count=" + newLoadouts.size(),
                e
            );
        } catch (Exception e) {
            throw new LoadoutBatchSaveException(
                "Batch insert failed for new loadouts: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "new loadouts count=" + newLoadouts.size(),
                e
            );
        }
    }

    /**
     * Performs batch update operation for existing Loadout entities with version increment.
     * This method fills common attributes, increments version numbers, and executes
     * a JOOQ batch update for all existing loadout records.
     * 
     * @param existingLoadouts collection of existing Loadout entities to update
     * @param cdmService service for filling common attributes
     * @throws LoadoutBatchSaveException if batch update fails
     * 
     * <p>Requirements: 2.2, 6.3, 8.1, 8.2</p>
     */
    public void batchUpdateLoadouts(Collection<Loadout> existingLoadouts, AbstractCDMService<Loadout> cdmService) throws LoadoutBatchSaveException {
        if (existingLoadouts == null || existingLoadouts.isEmpty()) {
            logger.debug("No existing loadouts to update, skipping batch update");
            return;
        }

        logger.debug("Preparing to batch update {} existing loadouts", existingLoadouts.size());
        
        try {
            // Fill common attributes and increment version for existing entities
            for (Loadout loadout : existingLoadouts) {
                cdmService.fillCommonAttributes(loadout);
                // Increment version for existing records
                Integer currentVersion = loadout.getVersion();
                loadout.setVersion(currentVersion != null ? currentVersion + 1 : INITIAL_VERSION);
                loadout.setOperationPerformed(ActionType.UPDATE);
            }

            // Convert to JOOQ update queries using proper JOOQ DSL
            List<org.jooq.Query> updateQueries = existingLoadouts.stream()
                .map(loadout -> {
                    com.salescode.dim.jooq.generated.tables.pojos.DmsLoadout dmsLoadout = loadout.getDmsLoadout();
                    
                    return dslContext
                        .update(DMS_LOADOUT)
                        // Update common fields
                        .set(DMS_LOADOUT.VERSION, loadout.getVersion())
                        .set(DMS_LOADOUT.ACTIVE_STATUS, dmsLoadout.getActiveStatus())
                        .set(DMS_LOADOUT.LAST_MODIFIED_TIME, loadout.getLastModifiedTime())
                        .set(DMS_LOADOUT.MODIFIED_BY, loadout.getModifiedBy())
                        // Update ALL DMS-specific fields from DmsLoadout POJO
                        .set(DMS_LOADOUT.LOAD_OUT_STATUS, dmsLoadout.getLoadOutStatus())
                        .set(DMS_LOADOUT.LOAD_OUT_TYPE, dmsLoadout.getLoadOutType())
                        .set(DMS_LOADOUT.SALESMAN_ID, dmsLoadout.getSalesmanId())
                        .set(DMS_LOADOUT.SUPPLIER, dmsLoadout.getSupplier())
                        .set(DMS_LOADOUT.TOTAL_AMOUNT, dmsLoadout.getTotalAmount())
                        .set(DMS_LOADOUT.TOTAL_CASE_LEFT_QTY, dmsLoadout.getTotalCaseLeftQty())
                        .set(DMS_LOADOUT.TOTAL_CASE_QTY, dmsLoadout.getTotalCaseQty())
                        .set(DMS_LOADOUT.TOTAL_OTHER_LEFT_QTY, dmsLoadout.getTotalOtherLeftQty())
                        .set(DMS_LOADOUT.TOTAL_OTHER_QTY, dmsLoadout.getTotalOtherQty())
                        .set(DMS_LOADOUT.TOTAL_PIECE_LEFT_QTY, dmsLoadout.getTotalPieceLeftQty())
                        .set(DMS_LOADOUT.TOTAL_PIECE_QTY, dmsLoadout.getTotalPieceQty())
                        .set(DMS_LOADOUT.VEHICLE_CAPACITY, dmsLoadout.getVehicleCapacity())
                        .set(DMS_LOADOUT.VEHICLE_ID, dmsLoadout.getVehicleId())
                        .set(DMS_LOADOUT.LOAD_OUT_DATE, dmsLoadout.getLoadOutDate())
                        .set(DMS_LOADOUT.LOADOUT_SOURCE, dmsLoadout.getLoadoutSource())
                        .set(DMS_LOADOUT.CASE_SHORTAGE, dmsLoadout.getCaseShortage())
                        .set(DMS_LOADOUT.OTHER_SHORTAGE, dmsLoadout.getOtherShortage())
                        .set(DMS_LOADOUT.PIECE_SHORTAGE, dmsLoadout.getPieceShortage())
                        .set(DMS_LOADOUT.DELIVERY_START_DATE, dmsLoadout.getDeliveryStartDate())
                        .set(DMS_LOADOUT.DELIVERY_END_DATE, dmsLoadout.getDeliveryEndDate())
                        .set(DMS_LOADOUT.SETTLEMENT_DATE, dmsLoadout.getSettlementDate())
                        .set(DMS_LOADOUT.SHORTAGE_UPDATED, dmsLoadout.getShortageUpdated())
                        .set(DMS_LOADOUT.INVOICE_CREATION_START_DATE, dmsLoadout.getInvoiceCreationStartDate())
                        .set(DMS_LOADOUT.INVOICE_CREATION_END_DATE, dmsLoadout.getInvoiceCreationEndDate())
                        .where(DMS_LOADOUT.ID.eq(dmsLoadout.getLoadNumber())); // Using ID since loadNumber is the primary key
                })
                .collect(Collectors.toList());

            // Execute batch update using JOOQ batch API
            int[] results = dslContext.batch(updateQueries).execute();
            
            int successCount = 0;
            for (int result : results) {
                if (result > 0) successCount++;
            }

            logger.info("Successfully batch updated {} existing loadouts ({} records affected)", existingLoadouts.size(), successCount);

        } catch (org.jooq.exception.DataAccessException e) {
            throw new LoadoutBatchSaveException(
                "Batch update failed for existing loadouts due to database error: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "existing loadouts count=" + existingLoadouts.size(),
                e
            );
        } catch (Exception e) {
            throw new LoadoutBatchSaveException(
                "Batch update failed for existing loadouts: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "existing loadouts count=" + existingLoadouts.size(),
                e
            );
        }
    }

    /**
     * Main method to perform batch upsert operations for Loadout entities.
     * This method orchestrates the complete upsert process: ensuring loadNumbers exist,
     * querying for existing records, separating new from existing, and executing
     * batch insert and update operations.
     * 
     * @param loadouts collection of Loadout entities to upsert
     * @param cdmService service for filling common attributes
     * @throws LoadoutBatchSaveException if any operation fails
     * 
     * <p>Requirements: 2.1, 2.2, 2.3, 6.1, 6.3, 8.1, 8.2</p>
     */
    public void batchUpsertLoadouts(Collection<Loadout> loadouts, AbstractCDMService<Loadout> cdmService) {
        if (loadouts == null || loadouts.isEmpty()) {
            return;
        }

        try {
            // Ensure all loadouts have loadNumbers
            hierarchyService.ensureLoadNumbers(loadouts);

            // Set loadNumber as ID for all loadouts (since loadNumber is the primary key)
            for (Loadout loadout : loadouts) {
                if (loadout.getDmsLoadout() != null && loadout.getDmsLoadout().getLoadNumber() != null) {
                    loadout.setId(loadout.getDmsLoadout().getLoadNumber());
                }
            }

            // Extract loadNumbers for querying existing records
            Set<String> loadNumbers = loadouts.stream()
                .map(loadout -> loadout.getDmsLoadout().getLoadNumber())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            // Query existing loadouts by loadNumber
            Map<String, Loadout> existingLoadoutsMap = queryExistingLoadoutsByLoadNumber(loadNumbers);

            // Separate into new and existing loadouts
            List<Loadout> newLoadouts = new ArrayList<>();
            List<Loadout> existingLoadouts = new ArrayList<>();

            for (Loadout loadout : loadouts) {
                String loadNumber = loadout.getDmsLoadout().getLoadNumber();
                if (existingLoadoutsMap.containsKey(loadNumber)) {
                    // Update existing loadout with new data while preserving database fields
                    Loadout existingLoadout = existingLoadoutsMap.get(loadNumber);
                    AbstractCDMService.fillAttributes(loadout, existingLoadout); // Copy non-null fields from input to existing
                    existingLoadouts.add(loadout);
                } else {
                    newLoadouts.add(loadout);
                }
            }

            // Perform batch operations
            batchInsertLoadouts(newLoadouts, cdmService);
            batchUpdateLoadouts(existingLoadouts, cdmService);

            logger.info("Completed batch upsert for {} loadouts ({} new, {} updated)", 
                       loadouts.size(), newLoadouts.size(), existingLoadouts.size());

        } catch (LoadoutBatchSaveException e) {
            // Just rethrow - exception already contains context
            throw e;
        } catch (Exception e) {
            throw new LoadoutBatchSaveException(
                "Batch upsert failed for loadouts: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "loadouts count=" + loadouts.size(),
                e
            );
        }
    }
}
