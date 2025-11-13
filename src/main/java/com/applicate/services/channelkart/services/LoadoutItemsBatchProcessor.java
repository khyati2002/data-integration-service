package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.impl.LoadoutItems;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.tables.DmsLoadoutItems.DMS_LOADOUT_ITEMS;

/**
 * Processor responsible for batch operations on LoadoutItems entities.
 * This class handles batch insert and update operations for LoadoutItems entities,
 * using a composite key (loadoutDetailsId + skuCode + batchCode + batchId + itemType)
 * for uniqueness checks.
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Validate required fields (loadoutDetailsId, skuCode)</li>
 *   <li>Query existing LoadoutItems by composite key</li>
 *   <li>Batch insert new LoadoutItems records</li>
 *   <li>Batch update existing LoadoutItems records with version increment</li>
 *   <li>Manage common attributes and audit fields</li>
 * </ul>
 * 
 * @see LoadoutHierarchyService
 * @see LoadoutValidationService
 * @see LoadoutItems
 */
public class LoadoutItemsBatchProcessor {

    private static final Logger logger = LoggerFactory.getLogger(LoadoutItemsBatchProcessor.class);
    private static final int INITIAL_VERSION = 1;
    
    private final DSLContext dslContext;
    private final LoadoutHierarchyService hierarchyService;
    private final LoadoutValidationService validationService;
    
    public LoadoutItemsBatchProcessor(DSLContext dslContext, LoadoutHierarchyService hierarchyService, LoadoutValidationService validationService) {
        this.dslContext = dslContext;
        this.hierarchyService = hierarchyService;
        this.validationService = validationService;
    }

    /**
     * Fills common attributes for LoadoutItems entities.
     * Sets creation time, last modified time, created by, modified by, and default itemType.
     * 
     * @param loadoutItem the LoadoutItems entity to fill attributes for
     * 
     * <p>Requirements: 6.1, 8.1, 8.2</p>
     */
    private void fillLoadoutItemsCommonAttributes(LoadoutItems loadoutItem) {
        if (loadoutItem.getCreationTime() == null) {
            loadoutItem.setCreationTime(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        }
        
        loadoutItem.setLastModifiedTime(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        
        if (loadoutItem.getCreatedBy() == null) {
            loadoutItem.setCreatedBy(com.applicate.services.channelkart.utils.SecurityContextUtils.getPrincipal());
        }
        if (loadoutItem.getModifiedBy() == null) {
            loadoutItem.setModifiedBy(com.applicate.services.channelkart.utils.SecurityContextUtils.getPrincipal());
        }
        
        // Default itemType to NORMAL if not explicitly set
        if (loadoutItem.getItemType() == null) {
            loadoutItem.setItemType(com.salescode.dim.jooq.generated.enums.DmsLoadoutItemsItemType.NORMAL);
        }
    }

    /**
     * Queries existing LoadoutItems by composite key (5 fields) using JOOQ.
     * The composite key consists of: loadoutDetailsId + skuCode + batchCode + batchId + itemType.
     * Handles nullable fields (batchCode, batchId, itemType) with proper null checks.
     * 
     * @param items collection of LoadoutItems to query for (used to build composite keys)
     * @return map of composite key string to LoadoutItems entity for all found records
     * @throws LoadoutBatchSaveException if database query fails
     * 
     * <p>Requirements: 4.2, 6.1, 6.2</p>
     */
    public Map<String, LoadoutItems> queryExistingLoadoutItemsByCompositeKey(Collection<LoadoutItems> items) {
        if (items == null || items.isEmpty()) {
            logger.debug("No LoadoutItems provided for query, returning empty map");
            return new HashMap<>();
        }

        logger.debug("Querying existing LoadoutItems for {} items using composite key", items.size());
        
        try {
            // Build a list of conditions for each composite key
            List<org.jooq.Condition> conditions = items.stream()
                .map(item -> {
                    org.jooq.Condition condition = DMS_LOADOUT_ITEMS.LOAD_OUT_DETAILS_ID.eq(item.getLoadOutDetailsId())
                        .and(DMS_LOADOUT_ITEMS.SKU_CODE.eq(item.getSkuCode()));
                    
                    // Add nullable fields with proper null handling
                    if (item.getBatchCode() != null) {
                        condition = condition.and(DMS_LOADOUT_ITEMS.BATCH_CODE.eq(item.getBatchCode()));
                    } else {
                        condition = condition.and(DMS_LOADOUT_ITEMS.BATCH_CODE.isNull());
                    }
                    
                    if (item.getBatchId() != null) {
                        condition = condition.and(DMS_LOADOUT_ITEMS.BATCH_ID.eq(item.getBatchId()));
                    } else {
                        condition = condition.and(DMS_LOADOUT_ITEMS.BATCH_ID.isNull());
                    }
                    
                    if (item.getItemType() != null) {
                        condition = condition.and(DMS_LOADOUT_ITEMS.ITEM_TYPE.eq(item.getItemType()));
                    } else {
                        condition = condition.and(DMS_LOADOUT_ITEMS.ITEM_TYPE.isNull());
                    }
                    
                    return condition;
                })
                .collect(Collectors.toList());

            // Combine all conditions with OR
            org.jooq.Condition combinedCondition = conditions.stream()
                .reduce(org.jooq.Condition::or)
                .orElse(org.jooq.impl.DSL.falseCondition());

            // Execute query using proper JOOQ DSL
            List<com.salescode.dim.jooq.generated.tables.pojos.DmsLoadoutItems> existingPojos = 
                dslContext
                    .selectFrom(DMS_LOADOUT_ITEMS)
                    .where(combinedCondition)
                    .fetchInto(com.salescode.dim.jooq.generated.tables.pojos.DmsLoadoutItems.class);

            // Convert to map using composite key
            Map<String, LoadoutItems> existingLoadoutItems = new HashMap<>();
            for (com.salescode.dim.jooq.generated.tables.pojos.DmsLoadoutItems pojo : existingPojos) {
                LoadoutItems loadoutItem = convertPojoToLoadoutItems(pojo);
                String compositeKey = hierarchyService.createLoadoutItemsCompositeKey(loadoutItem);
                existingLoadoutItems.put(compositeKey, loadoutItem);
            }

            logger.debug("Found {} existing LoadoutItems out of {} requested items", existingLoadoutItems.size(), items.size());
            return existingLoadoutItems;

        } catch (org.jooq.exception.DataAccessException e) {
            throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                "Database query failed for existing LoadoutItems: " + e.getMessage(),
                com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.DATABASE_ERROR,
                "items count=" + items.size(),
                e
            );
        } catch (Exception e) {
            throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                "Failed to query existing LoadoutItems: " + e.getMessage(),
                com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.DATABASE_ERROR,
                "items count=" + items.size(),
                e
            );
        }
    }

    /**
     * Converts JOOQ POJO to LoadoutItems entity.
     * Maps all fields from the database POJO to the domain entity.
     * 
     * @param pojo the JOOQ POJO from database query
     * @return LoadoutItems entity with all fields mapped
     */
    private LoadoutItems convertPojoToLoadoutItems(com.salescode.dim.jooq.generated.tables.pojos.DmsLoadoutItems pojo) {
        LoadoutItems loadoutItem = new LoadoutItems();
        loadoutItem.setId(pojo.getId());
        loadoutItem.setVersion(pojo.getVersion());
        loadoutItem.setActiveStatus(pojo.getActiveStatus());
        loadoutItem.setCreationTime(pojo.getCreationTime());
        loadoutItem.setLastModifiedTime(pojo.getLastModifiedTime());
        loadoutItem.setCreatedBy(pojo.getCreatedBy());
        loadoutItem.setModifiedBy(pojo.getModifiedBy());
        loadoutItem.setAmount(pojo.getAmount());
        loadoutItem.setBatchCode(pojo.getBatchCode());
        loadoutItem.setBatchId(pojo.getBatchId());
        loadoutItem.setCaseQty(pojo.getCaseQty());
        loadoutItem.setCaseQtyLeft(pojo.getCaseQtyLeft());
        loadoutItem.setLoadOutDetailsId(pojo.getLoadOutDetailsId());
        loadoutItem.setOtherQty(pojo.getOtherQty());
        loadoutItem.setOtherQtyLeft(pojo.getOtherQtyLeft());
        loadoutItem.setPieceQty(pojo.getPieceQty());
        loadoutItem.setPieceQtyLeft(pojo.getPieceQtyLeft());
        loadoutItem.setSkuCode(pojo.getSkuCode());
        loadoutItem.setMrp(pojo.getMrp());
        loadoutItem.setItemType(pojo.getItemType());
        return loadoutItem;
    }

    /**
     * Performs batch insert operation for new LoadoutItems entities.
     * Generates UUIDs for items without IDs, fills common attributes, and executes JOOQ batch insert.
     * 
     * @param newLoadoutItems collection of new LoadoutItems entities to insert
     * @throws LoadoutBatchSaveException if batch insert fails
     * 
     * <p>Requirements: 4.4, 6.3, 8.1, 8.2</p>
     */
    public void batchInsertLoadoutItems(Collection<LoadoutItems> newLoadoutItems) {
        if (newLoadoutItems == null || newLoadoutItems.isEmpty()) {
            logger.debug("No new LoadoutItems to insert, skipping batch insert");
            return;
        }

        logger.debug("Preparing to batch insert {} new LoadoutItems", newLoadoutItems.size());
        
        try {
            // Generate UUIDs for new LoadoutItems that don't have IDs
            int generatedIdCount = 0;
            for (LoadoutItems loadoutItem : newLoadoutItems) {
                if (StringUtils.isBlank(loadoutItem.getId())) {
                    loadoutItem.setId(UUID.randomUUID().toString());
                    generatedIdCount++;
                }
            }
            
            if (generatedIdCount > 0) {
                logger.debug("Generated {} UUIDs for new LoadoutItems", generatedIdCount);
            }

            // Convert to JOOQ records for batch insert using proper JOOQ DSL
            List<com.salescode.dim.jooq.generated.tables.records.DmsLoadoutItemsRecord> records = 
                newLoadoutItems.stream()
                    .map(loadoutItem -> {
                        com.salescode.dim.jooq.generated.tables.records.DmsLoadoutItemsRecord loadoutItemsRecord =
                            dslContext.newRecord(DMS_LOADOUT_ITEMS);
                        
                        // Map all common fields
                        loadoutItemsRecord.setId(loadoutItem.getId());
                        loadoutItemsRecord.setVersion(loadoutItem.getVersion() != null ? loadoutItem.getVersion() : INITIAL_VERSION);
                        loadoutItemsRecord.setActiveStatus(loadoutItem.getActiveStatus());
                        loadoutItemsRecord.setCreationTime(loadoutItem.getCreationTime());
                        loadoutItemsRecord.setLastModifiedTime(loadoutItem.getLastModifiedTime());
                        loadoutItemsRecord.setCreatedBy(loadoutItem.getCreatedBy());
                        loadoutItemsRecord.setModifiedBy(loadoutItem.getModifiedBy());
                        
                        // Map ALL DMS-specific fields from LoadoutItems
                        loadoutItemsRecord.setAmount(loadoutItem.getAmount());
                        loadoutItemsRecord.setBatchCode(loadoutItem.getBatchCode());
                        loadoutItemsRecord.setBatchId(loadoutItem.getBatchId());
                        loadoutItemsRecord.setCaseQty(loadoutItem.getCaseQty());
                        loadoutItemsRecord.setCaseQtyLeft(loadoutItem.getCaseQtyLeft());
                        loadoutItemsRecord.setLoadOutDetailsId(loadoutItem.getLoadOutDetailsId());
                        loadoutItemsRecord.setOtherQty(loadoutItem.getOtherQty());
                        loadoutItemsRecord.setOtherQtyLeft(loadoutItem.getOtherQtyLeft());
                        loadoutItemsRecord.setPieceQty(loadoutItem.getPieceQty());
                        loadoutItemsRecord.setPieceQtyLeft(loadoutItem.getPieceQtyLeft());
                        loadoutItemsRecord.setSkuCode(loadoutItem.getSkuCode());
                        loadoutItemsRecord.setMrp(loadoutItem.getMrp());
                        loadoutItemsRecord.setItemType(loadoutItem.getItemType());
                        
                        return loadoutItemsRecord;
                    })
                    .collect(Collectors.toList());

            // Execute batch insert using JOOQ batch API
            int[] results = dslContext.batchInsert(records).execute();
            
            int successCount = 0;
            for (int result : results) {
                if (result > 0) successCount++;
            }

            logger.info("Successfully batch inserted {} new LoadoutItems ({} records affected)", newLoadoutItems.size(), successCount);

        } catch (org.jooq.exception.DataAccessException e) {
            throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                "Batch insert failed for LoadoutItems due to database error: " + e.getMessage(),
                com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.DATABASE_ERROR,
                "new LoadoutItems count=" + newLoadoutItems.size(),
                e
            );
        } catch (Exception e) {
            throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                "Batch insert failed for LoadoutItems: " + e.getMessage(),
                com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.DATABASE_ERROR,
                "new LoadoutItems count=" + newLoadoutItems.size(),
                e
            );
        }
    }

    /**
     * Performs batch update operation for existing LoadoutItems entities with version increment.
     * Executes JOOQ batch update for all existing LoadoutItems records.
     * 
     * @param existingLoadoutItems collection of existing LoadoutItems entities to update
     * @throws LoadoutBatchSaveException if batch update fails
     * 
     * <p>Requirements: 4.3, 6.3, 8.1, 8.2</p>
     */
    public void batchUpdateLoadoutItems(Collection<LoadoutItems> existingLoadoutItems) {
        if (existingLoadoutItems == null || existingLoadoutItems.isEmpty()) {
            logger.debug("No existing LoadoutItems to update, skipping batch update");
            return;
        }

        logger.debug("Preparing to batch update {} existing LoadoutItems", existingLoadoutItems.size());
        
        try {
            // Convert to JOOQ update queries using proper JOOQ DSL
            List<org.jooq.Query> updateQueries = existingLoadoutItems.stream()
                .map(loadoutItem -> {
                    return dslContext
                        .update(DMS_LOADOUT_ITEMS)
                        // Update common fields
                        .set(DMS_LOADOUT_ITEMS.VERSION, loadoutItem.getVersion())
                        .set(DMS_LOADOUT_ITEMS.ACTIVE_STATUS, loadoutItem.getActiveStatus())
                        .set(DMS_LOADOUT_ITEMS.LAST_MODIFIED_TIME, loadoutItem.getLastModifiedTime())
                        .set(DMS_LOADOUT_ITEMS.MODIFIED_BY, loadoutItem.getModifiedBy())
                        // Update ALL DMS-specific fields
                        .set(DMS_LOADOUT_ITEMS.AMOUNT, loadoutItem.getAmount())
                        .set(DMS_LOADOUT_ITEMS.BATCH_CODE, loadoutItem.getBatchCode())
                        .set(DMS_LOADOUT_ITEMS.BATCH_ID, loadoutItem.getBatchId())
                        .set(DMS_LOADOUT_ITEMS.CASE_QTY, loadoutItem.getCaseQty())
                        .set(DMS_LOADOUT_ITEMS.CASE_QTY_LEFT, loadoutItem.getCaseQtyLeft())
                        .set(DMS_LOADOUT_ITEMS.OTHER_QTY, loadoutItem.getOtherQty())
                        .set(DMS_LOADOUT_ITEMS.OTHER_QTY_LEFT, loadoutItem.getOtherQtyLeft())
                        .set(DMS_LOADOUT_ITEMS.PIECE_QTY, loadoutItem.getPieceQty())
                        .set(DMS_LOADOUT_ITEMS.PIECE_QTY_LEFT, loadoutItem.getPieceQtyLeft())
                        .set(DMS_LOADOUT_ITEMS.MRP, loadoutItem.getMrp())
                        .set(DMS_LOADOUT_ITEMS.ITEM_TYPE, loadoutItem.getItemType())
                        .where(DMS_LOADOUT_ITEMS.ID.eq(loadoutItem.getId()));
                })
                .collect(Collectors.toList());

            // Execute batch update using JOOQ batch API
            int[] results = dslContext.batch(updateQueries).execute();
            
            int successCount = 0;
            for (int result : results) {
                if (result > 0) successCount++;
            }

            logger.info("Successfully batch updated {} existing LoadoutItems ({} records affected)", existingLoadoutItems.size(), successCount);

        } catch (org.jooq.exception.DataAccessException e) {
            throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                "Batch update failed for LoadoutItems due to database error: " + e.getMessage(),
                com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.DATABASE_ERROR,
                "existing LoadoutItems count=" + existingLoadoutItems.size(),
                e
            );
        } catch (Exception e) {
            throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                "Batch update failed for LoadoutItems: " + e.getMessage(),
                com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.DATABASE_ERROR,
                "existing LoadoutItems count=" + existingLoadoutItems.size(),
                e
            );
        }
    }

    /**
     * Main method to perform batch upsert operations for LoadoutItems entities.
     * Orchestrates the complete upsert process: validating required fields,
     * querying for existing records by composite key, separating new from existing,
     * and executing batch insert and update operations.
     * 
     * @param loadoutItems collection of LoadoutItems entities to upsert
     * @throws LoadoutBatchSaveException if validation or any operation fails
     * 
     * <p>Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 6.1, 6.3, 8.1, 8.2</p>
     */
    public void batchUpsertLoadoutItems(Collection<LoadoutItems> loadoutItems) {
        if (loadoutItems == null || loadoutItems.isEmpty()) {
            return;
        }

        try {
            // Validate required fields for all items
            validationService.validateLoadoutItemsCollection(loadoutItems);

            // Query existing LoadoutItems by composite key
            Map<String, LoadoutItems> existingLoadoutItemsMap = queryExistingLoadoutItemsByCompositeKey(loadoutItems);

            // Separate into new and existing LoadoutItems
            List<LoadoutItems> newLoadoutItems = new ArrayList<>();
            List<LoadoutItems> existingLoadoutItems = new ArrayList<>();

            for (LoadoutItems loadoutItem : loadoutItems) {
                String compositeKey = hierarchyService.createLoadoutItemsCompositeKey(loadoutItem);
                if (existingLoadoutItemsMap.containsKey(compositeKey)) {
                    // Update existing LoadoutItems with new data while preserving database fields
                    LoadoutItems existingLoadoutItem = existingLoadoutItemsMap.get(compositeKey);
                    AbstractCDMService.fillAttributes(existingLoadoutItem, loadoutItem); // Copy non-null fields from input to existing
                    
                    // Fill common attributes and increment version for existing entities
                    fillLoadoutItemsCommonAttributes(existingLoadoutItem);
                    Integer currentVersion = existingLoadoutItem.getVersion();
                    existingLoadoutItem.setVersion(currentVersion != null ? currentVersion + 1 : INITIAL_VERSION);
                    
                    existingLoadoutItems.add(existingLoadoutItem);
                } else {
                    // Fill common attributes for new entities
                    fillLoadoutItemsCommonAttributes(loadoutItem);
                    if (loadoutItem.getVersion() == null) {
                        loadoutItem.setVersion(INITIAL_VERSION);
                    }
                    newLoadoutItems.add(loadoutItem);
                }
            }

            // Perform batch operations
            batchInsertLoadoutItems(newLoadoutItems);
            batchUpdateLoadoutItems(existingLoadoutItems);

            logger.info("Completed batch upsert for {} loadout items ({} new, {} updated)", 
                       loadoutItems.size(), newLoadoutItems.size(), existingLoadoutItems.size());

        } catch (com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException e) {
            // Just rethrow - exception already contains context
            throw e;
        } catch (Exception e) {
            throw new com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException(
                "Batch upsert failed for loadout items: " + e.getMessage(),
                com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType.DATABASE_ERROR,
                "loadout items count=" + loadoutItems.size(),
                e
            );
        }
    }
}
