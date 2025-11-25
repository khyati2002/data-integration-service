package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.impl.LoadoutDetails;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.tables.DmsLoadoutDetails.DMS_LOADOUT_DETAILS;

/**
 * Processor responsible for batch operations on LoadoutDetails entities.
 * This class handles batch insert and update operations for LoadoutDetails entities,
 * using composite IDs (loadNumber-invoiceNumber) for uniqueness checks.
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Query existing LoadoutDetails by composite ID</li>
 *   <li>Batch insert new LoadoutDetails records</li>
 *   <li>Batch update existing LoadoutDetails records with version increment</li>
 *   <li>Manage common attributes and audit fields</li>
 * </ul>
 * 
 * @see LoadoutHierarchyService
 * @see LoadoutDetails
 */
public class LoadoutDetailsBatchProcessor {

    private static final Logger logger = LoggerFactory.getLogger(LoadoutDetailsBatchProcessor.class);
    private static final int INITIAL_VERSION = 1;
    
    private final DSLContext dslContext;
    private final LoadoutHierarchyService hierarchyService;
    
    public LoadoutDetailsBatchProcessor(DSLContext dslContext, LoadoutHierarchyService hierarchyService) {
        this.dslContext = dslContext;
        this.hierarchyService = hierarchyService;
    }

    /**
     * Fills common attributes for LoadoutDetails entities (non-CommonDataModel).
     * Sets creation time, last modified time, created by, and modified by fields
     * following the same pattern as AbstractCDMService.fillCommonAttributes.
     * 
     * @param loadoutDetail the LoadoutDetails entity to fill attributes for
     * 
     * <p>Requirements: 6.1, 8.1</p>
     */
    private void fillLoadoutDetailsCommonAttributes(LoadoutDetails loadoutDetail) {
        if (loadoutDetail.getCreationTime() == null) {
            loadoutDetail.setCreationTime(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        }
        
        loadoutDetail.setLastModifiedTime(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        
        if (loadoutDetail.getCreatedBy() == null) {
            loadoutDetail.setCreatedBy(com.applicate.services.channelkart.utils.SecurityContextUtils.getPrincipal());
        }
        if (loadoutDetail.getModifiedBy() == null) {
            loadoutDetail.setModifiedBy(com.applicate.services.channelkart.utils.SecurityContextUtils.getPrincipal());
        }
    }

    /**
     * Queries existing LoadoutDetails by composite ID using JOOQ.
     * The composite ID format is "loadNumber-invoiceNumber".
     * 
     * @param compositeIds collection of composite IDs to query for
     * @return map of composite ID to LoadoutDetails entity for all found records
     * @throws LoadoutBatchSaveException if database query fails
     * 
     * <p>Requirements: 3.2, 6.1, 6.2</p>
     */
    public Map<String, LoadoutDetails> queryExistingLoadoutDetailsByCompositeId(Collection<String> compositeIds) {
        if (compositeIds == null || compositeIds.isEmpty()) {
            logger.debug("No composite IDs provided for query, returning empty map");
            return new HashMap<>();
        }

        logger.debug("Querying existing LoadoutDetails for {} composite IDs", compositeIds.size());
        
        try {
            // Using proper JOOQ DSL with table constants
            List<com.salescode.dim.jooq.generated.tables.pojos.DmsLoadoutDetails> existingPojos = 
                dslContext
                    .selectFrom(DMS_LOADOUT_DETAILS)
                    .where(DMS_LOADOUT_DETAILS.ID.in(compositeIds))
                    .fetchInto(com.salescode.dim.jooq.generated.tables.pojos.DmsLoadoutDetails.class);

            Map<String, LoadoutDetails> existingLoadoutDetails = new HashMap<>();
            for (com.salescode.dim.jooq.generated.tables.pojos.DmsLoadoutDetails pojo : existingPojos) {
                LoadoutDetails loadoutDetail = convertPojoToLoadoutDetails(pojo);
                existingLoadoutDetails.put(pojo.getId(), loadoutDetail);
            }

            logger.debug("Found {} existing LoadoutDetails out of {} requested composite IDs", existingLoadoutDetails.size(), compositeIds.size());
            return existingLoadoutDetails;

        } catch (org.jooq.exception.DataAccessException e) {
            throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                "Database query failed for existing LoadoutDetails: " + e.getMessage(),
                com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.DATABASE_ERROR,
                "composite IDs count=" + compositeIds.size(),
                e
            );
        } catch (Exception e) {
            throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                "Failed to query existing LoadoutDetails: " + e.getMessage(),
                com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.DATABASE_ERROR,
                "composite IDs count=" + compositeIds.size(),
                e
            );
        }
    }

    /**
     * Converts JOOQ POJO to LoadoutDetails entity.
     * Maps all fields from the database POJO to the domain entity.
     * 
     * @param pojo the JOOQ POJO from database query
     * @return LoadoutDetails entity with all fields mapped
     */
    private LoadoutDetails convertPojoToLoadoutDetails(com.salescode.dim.jooq.generated.tables.pojos.DmsLoadoutDetails pojo) {
        LoadoutDetails loadoutDetail = new LoadoutDetails();
        loadoutDetail.setId(pojo.getId());
        loadoutDetail.setVersion(pojo.getVersion());
        loadoutDetail.setActiveStatus(pojo.getActiveStatus());
        loadoutDetail.setCreationTime(pojo.getCreationTime());
        loadoutDetail.setLastModifiedTime(pojo.getLastModifiedTime());
        loadoutDetail.setCreatedBy(pojo.getCreatedBy());
        loadoutDetail.setModifiedBy(pojo.getModifiedBy());
        loadoutDetail.setInvoiceNumber(pojo.getInvoiceNumber());
        loadoutDetail.setLoadNumber(pojo.getLoadNumber());
        loadoutDetail.setLoadOutStatus(pojo.getLoadOutStatus());
        loadoutDetail.setOutletCode(pojo.getOutletCode());
        loadoutDetail.setPresellerId(pojo.getPresellerId());
        loadoutDetail.setRouteCode(pojo.getRouteCode());
        loadoutDetail.setSalesInfo(pojo.getSalesInfo());
        loadoutDetail.setTotalAmount(pojo.getTotalAmount());
        loadoutDetail.setTotalCaseLeftQty(pojo.getTotalCaseLeftQty());
        loadoutDetail.setTotalCaseQty(pojo.getTotalCaseQty());
        loadoutDetail.setTotalOtherLeftQty(pojo.getTotalOtherLeftQty());
        loadoutDetail.setTotalOtherQty(pojo.getTotalOtherQty());
        loadoutDetail.setTotalPieceLeftQty(pojo.getTotalPieceLeftQty());
        loadoutDetail.setTotalPieceQty(pojo.getTotalPieceQty());
        loadoutDetail.setReturnCaseQty(pojo.getReturnCaseQty());
        loadoutDetail.setReturnOtherQty(pojo.getReturnOtherQty());
        loadoutDetail.setReturnPieceQty(pojo.getReturnPieceQty());
        loadoutDetail.setInvSerNo(pojo.getInvSerNo());
        return loadoutDetail;
    }

    /**
     * Performs batch insert operation for new LoadoutDetails entities.
     * Fills common attributes, sets initial version, and executes JOOQ batch insert.
     * 
     * @param newLoadoutDetails collection of new LoadoutDetails entities to insert
     * @throws LoadoutBatchSaveException if batch insert fails
     * 
     * <p>Requirements: 3.3, 6.3, 8.1, 8.2</p>
     */
    public void batchInsertLoadoutDetails(Collection<LoadoutDetails> newLoadoutDetails) {
        if (newLoadoutDetails == null || newLoadoutDetails.isEmpty()) {
            logger.debug("No new LoadoutDetails to insert, skipping batch insert");
            return;
        }

        logger.debug("Preparing to batch insert {} new LoadoutDetails", newLoadoutDetails.size());
        
        try {


            // Convert to JOOQ records for batch insert using proper JOOQ DSL
            List<com.salescode.dim.jooq.generated.tables.records.DmsLoadoutDetailsRecord> records = 
                newLoadoutDetails.stream()
                    .map(loadoutDetail -> {
                        com.salescode.dim.jooq.generated.tables.records.DmsLoadoutDetailsRecord loadoutDetailsRecord =
                            dslContext.newRecord(DMS_LOADOUT_DETAILS);
                        
                        // Map all common fields
                        loadoutDetailsRecord.setId(loadoutDetail.getId());
                        loadoutDetailsRecord.setVersion(loadoutDetail.getVersion() != null ? loadoutDetail.getVersion() : INITIAL_VERSION);
                        loadoutDetailsRecord.setActiveStatus(loadoutDetail.getActiveStatus());
                        loadoutDetailsRecord.setCreationTime(loadoutDetail.getCreationTime());
                        loadoutDetailsRecord.setLastModifiedTime(loadoutDetail.getLastModifiedTime());
                        loadoutDetailsRecord.setCreatedBy(loadoutDetail.getCreatedBy());
                        loadoutDetailsRecord.setModifiedBy(loadoutDetail.getModifiedBy());
                        
                        // Map ALL DMS-specific fields from LoadoutDetails
                        loadoutDetailsRecord.setInvoiceNumber(loadoutDetail.getInvoiceNumber());
                        loadoutDetailsRecord.setLoadNumber(loadoutDetail.getLoadNumber());
                        loadoutDetailsRecord.setLoadOutStatus(loadoutDetail.getLoadOutStatus());
                        loadoutDetailsRecord.setOutletCode(loadoutDetail.getOutletCode());
                        loadoutDetailsRecord.setPresellerId(loadoutDetail.getPresellerId());
                        loadoutDetailsRecord.setRouteCode(loadoutDetail.getRouteCode());
                        loadoutDetailsRecord.setSalesInfo(loadoutDetail.getSalesInfo());
                        loadoutDetailsRecord.setTotalAmount(loadoutDetail.getTotalAmount());
                        loadoutDetailsRecord.setTotalCaseLeftQty(loadoutDetail.getTotalCaseLeftQty());
                        loadoutDetailsRecord.setTotalCaseQty(loadoutDetail.getTotalCaseQty());
                        loadoutDetailsRecord.setTotalOtherLeftQty(loadoutDetail.getTotalOtherLeftQty());
                        loadoutDetailsRecord.setTotalOtherQty(loadoutDetail.getTotalOtherQty());
                        loadoutDetailsRecord.setTotalPieceLeftQty(loadoutDetail.getTotalPieceLeftQty());
                        loadoutDetailsRecord.setTotalPieceQty(loadoutDetail.getTotalPieceQty());
                        loadoutDetailsRecord.setReturnCaseQty(loadoutDetail.getReturnCaseQty());
                        loadoutDetailsRecord.setReturnOtherQty(loadoutDetail.getReturnOtherQty());
                        loadoutDetailsRecord.setReturnPieceQty(loadoutDetail.getReturnPieceQty());
                        loadoutDetailsRecord.setInvSerNo(loadoutDetail.getInvSerNo());
                        
                        return loadoutDetailsRecord;
                    })
                    .collect(Collectors.toList());

            // Execute batch insert using JOOQ batch API
            int[] results = dslContext.batchInsert(records).execute();
            
            int successCount = 0;
            for (int result : results) {
                if (result > 0) successCount++;
            }

            logger.info("Successfully batch inserted {} new LoadoutDetails ({} records affected)", newLoadoutDetails.size(), successCount);

        } catch (org.jooq.exception.DataAccessException e) {
            throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                "Batch insert failed for LoadoutDetails due to database error: " + e.getMessage(),
                com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.DATABASE_ERROR,
                "new LoadoutDetails count=" + newLoadoutDetails.size(),
                e
            );
        } catch (Exception e) {
            throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                "Batch insert failed for LoadoutDetails: " + e.getMessage(),
                com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.DATABASE_ERROR,
                "new LoadoutDetails count=" + newLoadoutDetails.size(),
                e
            );
        }
    }

    /**
     * Performs batch update operation for existing LoadoutDetails entities with version increment.
     * Fills common attributes, increments version numbers, and executes JOOQ batch update.
     * 
     * @param existingLoadoutDetails collection of existing LoadoutDetails entities to update
     * @throws LoadoutBatchSaveException if batch update fails
     * 
     * <p>Requirements: 3.3, 6.3, 8.1, 8.2</p>
     */
    public void batchUpdateLoadoutDetails(Collection<LoadoutDetails> existingLoadoutDetails) {
        if (existingLoadoutDetails == null || existingLoadoutDetails.isEmpty()) {
            logger.debug("No existing LoadoutDetails to update, skipping batch update");
            return;
        }

        logger.debug("Preparing to batch update {} existing LoadoutDetails", existingLoadoutDetails.size());
        
        try {


            // Convert to JOOQ update queries using proper JOOQ DSL
            List<org.jooq.Query> updateQueries = existingLoadoutDetails.stream()
                .map(loadoutDetail -> {
                    return dslContext
                        .update(DMS_LOADOUT_DETAILS)
                        // Update common fields
                        .set(DMS_LOADOUT_DETAILS.VERSION, loadoutDetail.getVersion())
                        .set(DMS_LOADOUT_DETAILS.ACTIVE_STATUS, loadoutDetail.getActiveStatus())
                        .set(DMS_LOADOUT_DETAILS.LAST_MODIFIED_TIME, loadoutDetail.getLastModifiedTime())
                        .set(DMS_LOADOUT_DETAILS.MODIFIED_BY, loadoutDetail.getModifiedBy())
                        // Update ALL DMS-specific fields
                        .set(DMS_LOADOUT_DETAILS.LOAD_OUT_STATUS, loadoutDetail.getLoadOutStatus())
                        .set(DMS_LOADOUT_DETAILS.OUTLET_CODE, loadoutDetail.getOutletCode())
                        .set(DMS_LOADOUT_DETAILS.PRESELLER_ID, loadoutDetail.getPresellerId())
                        .set(DMS_LOADOUT_DETAILS.ROUTE_CODE, loadoutDetail.getRouteCode())
                        .set(DMS_LOADOUT_DETAILS.SALES_INFO, loadoutDetail.getSalesInfo())
                        .set(DMS_LOADOUT_DETAILS.TOTAL_AMOUNT, loadoutDetail.getTotalAmount())
                        .set(DMS_LOADOUT_DETAILS.TOTAL_CASE_LEFT_QTY, loadoutDetail.getTotalCaseLeftQty())
                        .set(DMS_LOADOUT_DETAILS.TOTAL_CASE_QTY, loadoutDetail.getTotalCaseQty())
                        .set(DMS_LOADOUT_DETAILS.TOTAL_OTHER_LEFT_QTY, loadoutDetail.getTotalOtherLeftQty())
                        .set(DMS_LOADOUT_DETAILS.TOTAL_OTHER_QTY, loadoutDetail.getTotalOtherQty())
                        .set(DMS_LOADOUT_DETAILS.TOTAL_PIECE_LEFT_QTY, loadoutDetail.getTotalPieceLeftQty())
                        .set(DMS_LOADOUT_DETAILS.TOTAL_PIECE_QTY, loadoutDetail.getTotalPieceQty())
                        .set(DMS_LOADOUT_DETAILS.RETURN_CASE_QTY, loadoutDetail.getReturnCaseQty())
                        .set(DMS_LOADOUT_DETAILS.RETURN_OTHER_QTY, loadoutDetail.getReturnOtherQty())
                        .set(DMS_LOADOUT_DETAILS.RETURN_PIECE_QTY, loadoutDetail.getReturnPieceQty())
                        .set(DMS_LOADOUT_DETAILS.INV_SER_NO, loadoutDetail.getInvSerNo())
                        .where(DMS_LOADOUT_DETAILS.ID.eq(loadoutDetail.getId()));
                })
                .collect(Collectors.toList());

            // Execute batch update using JOOQ batch API
            int[] results = dslContext.batch(updateQueries).execute();
            
            int successCount = 0;
            for (int result : results) {
                if (result > 0) successCount++;
            }

            logger.info("Successfully batch updated {} existing LoadoutDetails ({} records affected)", existingLoadoutDetails.size(), successCount);

        } catch (org.jooq.exception.DataAccessException e) {
            throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                "Batch update failed for LoadoutDetails due to database error: " + e.getMessage(),
                com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.DATABASE_ERROR,
                "existing LoadoutDetails count=" + existingLoadoutDetails.size(),
                e
            );
        } catch (Exception e) {
            throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                "Batch update failed for LoadoutDetails: " + e.getMessage(),
                com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.DATABASE_ERROR,
                "existing LoadoutDetails count=" + existingLoadoutDetails.size(),
                e
            );
        }
    }

    /**
     * Main method to perform batch upsert operations for LoadoutDetails entities.
     * Orchestrates the complete upsert process: generating composite IDs,
     * querying for existing records, separating new from existing, and executing
     * batch insert and update operations.
     * 
     * @param loadoutDetails collection of LoadoutDetails entities to upsert
     * @throws LoadoutBatchSaveException if any operation fails
     * 
     * <p>Requirements: 3.1, 3.2, 3.3, 6.1, 6.3, 8.1, 8.2</p>
     */
    public void batchUpsertLoadoutDetails(Collection<LoadoutDetails> loadoutDetails) {
        if (loadoutDetails == null || loadoutDetails.isEmpty()) {
            return;
        }

        try {

            // Extract composite IDs for querying existing records
            Set<String> compositeIds = loadoutDetails.stream()
                .map(LoadoutDetails::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            // Query existing LoadoutDetails by composite ID
            Map<String, LoadoutDetails> existingLoadoutDetailsMap = queryExistingLoadoutDetailsByCompositeId(compositeIds);

            // Separate into new and existing LoadoutDetails
            List<LoadoutDetails> newLoadoutDetails = new ArrayList<>();
            List<LoadoutDetails> existingLoadoutDetails = new ArrayList<>();

            for (LoadoutDetails loadoutDetail : loadoutDetails) {
                String compositeId = loadoutDetail.getId();
                if (existingLoadoutDetailsMap.containsKey(compositeId)) {
                    // Update existing LoadoutDetails with new data while preserving database fields
                    LoadoutDetails existingLoadoutDetail = existingLoadoutDetailsMap.get(compositeId);
                    AbstractCDMService.fillAttributes(loadoutDetail, existingLoadoutDetail); // Copy non-null fields from input to existing

                    fillLoadoutDetailsCommonAttributes(loadoutDetail);
                    Integer currentVersion = existingLoadoutDetail.getVersion();
                    loadoutDetail.setVersion(currentVersion != null ? currentVersion + 1 : INITIAL_VERSION);

                    existingLoadoutDetails.add(loadoutDetail);
                } else {
                    fillLoadoutDetailsCommonAttributes(loadoutDetail);;
                    if (loadoutDetail.getVersion() == null) {
                        loadoutDetail.setVersion(INITIAL_VERSION);
                    }

                    newLoadoutDetails.add(loadoutDetail);
                }
            }

            // Perform batch operations
            batchInsertLoadoutDetails(newLoadoutDetails);
            batchUpdateLoadoutDetails(existingLoadoutDetails);

            logger.info("Completed batch upsert for {} loadout details ({} new, {} updated)", 
                       loadoutDetails.size(), newLoadoutDetails.size(), existingLoadoutDetails.size());

        } catch (com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException e) {
            // Just rethrow - exception already contains context
            throw e;
        } catch (Exception e) {
            throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                "Batch upsert failed for loadout details: " + e.getMessage(),
                com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.DATABASE_ERROR,
                "loadout details count=" + loadoutDetails.size(),
                e
            );
        }
    }
}
