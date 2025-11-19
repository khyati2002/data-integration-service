package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException;
import com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException.ErrorType;
import com.salescode.dim.jooq.impl.VanItems;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.tables.DmsVanItems.DMS_VAN_ITEMS;

/**
 * Processor responsible for batch operations on VanItems entities.
 * This class handles batch insert and update operations for VanItems entities,
 * using a composite key (loadNumber + skuCode + batchCode + batchId + itemType)
 * for uniqueness checks.
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Validate required fields (loadNumber, skuCode)</li>
 *   <li>Query existing VanItems by composite key</li>
 *   <li>Batch insert new VanItems records with UUID generation</li>
 *   <li>Batch update existing VanItems records with version increment</li>
 *   <li>Manage common attributes and audit fields</li>
 * </ul>
 * 
 * @see VanLoadoutHierarchyService
 * @see VanLoadoutValidationService
 * @see VanItems
 */
public class VanItemsBatchProcessor {

    private static final Logger logger = LoggerFactory.getLogger(VanItemsBatchProcessor.class);
    private static final int INITIAL_VERSION = 1;
    
    private final DSLContext dslContext;
    private final VanLoadoutHierarchyService hierarchyService;
    private final VanLoadoutValidationService validationService;
    
    public VanItemsBatchProcessor(DSLContext dslContext, VanLoadoutHierarchyService hierarchyService, VanLoadoutValidationService validationService) {
        this.dslContext = dslContext;
        this.hierarchyService = hierarchyService;
        this.validationService = validationService;
    }

    /**
     * Fills common attributes for VanItems entities.
     * Sets creation time, last modified time, created by, modified by, and default itemType.
     * 
     * @param vanItem the VanItems entity to fill attributes for
     *
     */
    private void fillVanItemsCommonAttributes(VanItems vanItem) {
        if (vanItem.getCreationTime() == null) {
            vanItem.setCreationTime(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        }
        
        vanItem.setLastModifiedTime(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        
        if (vanItem.getCreatedBy() == null) {
            vanItem.setCreatedBy(com.applicate.services.channelkart.utils.SecurityContextUtils.getPrincipal());
        }
        if (vanItem.getModifiedBy() == null) {
            vanItem.setModifiedBy(com.applicate.services.channelkart.utils.SecurityContextUtils.getPrincipal());
        }
        
        // Default itemType to NORMAL if not explicitly set
        if (vanItem.getItemType() == null) {
            vanItem.setItemType(com.salescode.dim.jooq.generated.enums.DmsVanItemsItemType.NORMAL);
        }
    }

    /**
     * Queries existing VanItems by composite key (5 fields) using JOOQ.
     * The composite key consists of: loadNumber + skuCode + batchCode + batchId + itemType.
     * Handles nullable fields (batchCode, batchId, itemType) with proper null checks.
     * 
     * @param items collection of VanItems to query for (used to build composite keys)
     * @return map of composite key string to VanItems entity for all found records (includes UUID id from database)
     * @throws LoadoutBatchSaveException if database query fails
     *
     */
    public Map<String, VanItems> queryExistingVanItemsByCompositeKey(Collection<VanItems> items) {
        if (items == null || items.isEmpty()) {
            logger.debug("No VanItems provided for query, returning empty map");
            return new HashMap<>();
        }

        logger.debug("Querying existing VanItems for {} items using composite key", items.size());
        
        try {
            // Build a list of conditions for each composite key
            List<org.jooq.Condition> conditions = items.stream()
                .map(item -> {
                    org.jooq.Condition condition = DMS_VAN_ITEMS.LOAD_NUMBER.eq(item.getLoadNumber())
                        .and(DMS_VAN_ITEMS.SKU_CODE.eq(item.getSkuCode()));
                    
                    // Add nullable fields with proper null handling
                    if (item.getBatchCode() != null) {
                        condition = condition.and(DMS_VAN_ITEMS.BATCH_CODE.eq(item.getBatchCode()));
                    } else {
                        condition = condition.and(DMS_VAN_ITEMS.BATCH_CODE.isNull());
                    }
                    
                    if (item.getBatchId() != null) {
                        condition = condition.and(DMS_VAN_ITEMS.BATCH_ID.eq(item.getBatchId()));
                    } else {
                        condition = condition.and(DMS_VAN_ITEMS.BATCH_ID.isNull());
                    }
                    
                    if (item.getItemType() != null) {
                        condition = condition.and(DMS_VAN_ITEMS.ITEM_TYPE.eq(item.getItemType()));
                    } else {
                        condition = condition.and(DMS_VAN_ITEMS.ITEM_TYPE.isNull());
                    }
                    
                    return condition;
                })
                .collect(Collectors.toList());

            // Combine all conditions with OR
            org.jooq.Condition combinedCondition = conditions.stream()
                .reduce(org.jooq.Condition::or)
                .orElse(org.jooq.impl.DSL.falseCondition());

            // Execute query using proper JOOQ DSL
            List<com.salescode.dim.jooq.generated.tables.pojos.DmsVanItems> existingPojos = 
                dslContext
                    .selectFrom(DMS_VAN_ITEMS)
                    .where(combinedCondition)
                    .fetchInto(com.salescode.dim.jooq.generated.tables.pojos.DmsVanItems.class);

            // Convert to map using composite key
            Map<String, VanItems> existingVanItems = new HashMap<>();
            for (com.salescode.dim.jooq.generated.tables.pojos.DmsVanItems pojo : existingPojos) {
                VanItems vanItem = convertPojoToVanItems(pojo);
                String compositeKey = hierarchyService.createVanItemsCompositeKey(vanItem);
                existingVanItems.put(compositeKey, vanItem);
            }

            logger.debug("Found {} existing VanItems out of {} requested items", existingVanItems.size(), items.size());
            return existingVanItems;

        } catch (org.jooq.exception.DataAccessException e) {
            throw new LoadoutBatchSaveException(
                "Database query failed for existing VanItems: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "items count=" + items.size(),
                e
            );
        } catch (Exception e) {
            throw new LoadoutBatchSaveException(
                "Failed to query existing VanItems: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "items count=" + items.size(),
                e
            );
        }
    }

    /**
     * Converts JOOQ POJO to VanItems entity.
     * Maps all fields from the database POJO to the domain entity.
     * 
     * @param pojo the JOOQ POJO from database query
     * @return VanItems entity with all fields mapped
     */
    private VanItems convertPojoToVanItems(com.salescode.dim.jooq.generated.tables.pojos.DmsVanItems pojo) {
        VanItems vanItem = new VanItems();
        vanItem.setId(pojo.getId());
        vanItem.setVersion(pojo.getVersion());
        vanItem.setActiveStatus(pojo.getActiveStatus());
        vanItem.setCreationTime(pojo.getCreationTime());
        vanItem.setLastModifiedTime(pojo.getLastModifiedTime());
        vanItem.setCreatedBy(pojo.getCreatedBy());
        vanItem.setModifiedBy(pojo.getModifiedBy());
        vanItem.setBatchCode(pojo.getBatchCode());
        vanItem.setSkuCode(pojo.getSkuCode());
        vanItem.setBatchId(pojo.getBatchId());
        vanItem.setItemType(pojo.getItemType());
        vanItem.setLoadNumber(pojo.getLoadNumber());
        vanItem.setMrp(pojo.getMrp());
        vanItem.setBasePrice(pojo.getBasePrice());
        vanItem.setCaseQty(pojo.getCaseQty());
        vanItem.setPieceQty(pojo.getPieceQty());
        vanItem.setOtherQty(pojo.getOtherQty());
        vanItem.setCaseQtyLeft(pojo.getCaseQtyLeft());
        vanItem.setPieceQtyLeft(pojo.getPieceQtyLeft());
        vanItem.setOtherQtyLeft(pojo.getOtherQtyLeft());
        vanItem.setSuggestedPieceQty(pojo.getSuggestedPieceQty());
        vanItem.setSuggestedCaseQty(pojo.getSuggestedCaseQty());
        vanItem.setSuggestedOtherQty(pojo.getSuggestedOtherQty());
        vanItem.setAcceptedPieceQty(pojo.getAcceptedPieceQty());
        vanItem.setAcceptedCaseQty(pojo.getAcceptedCaseQty());
        vanItem.setAcceptedOtherQty(pojo.getAcceptedOtherQty());
        return vanItem;
    }

    /**
     * Performs batch insert operation for new VanItems entities.
     * Generates UUIDs for items without IDs, fills common attributes, and executes JOOQ batch insert.
     * 
     * @param newVanItems collection of new VanItems entities to insert
     * @throws LoadoutBatchSaveException if batch insert fails
     *
     */
    public void batchInsertVanItems(Collection<VanItems> newVanItems) {
        if (newVanItems == null || newVanItems.isEmpty()) {
            logger.debug("No new VanItems to insert, skipping batch insert");
            return;
        }

        logger.debug("Preparing to batch insert {} new VanItems", newVanItems.size());
        
        try {
            // Generate UUIDs for new VanItems that don't have IDs
            int generatedIdCount = 0;
            for (VanItems vanItem : newVanItems) {
                if (StringUtils.isBlank(vanItem.getId())) {
                    vanItem.setId(UUID.randomUUID().toString());
                    generatedIdCount++;
                }
            }
            
            if (generatedIdCount > 0) {
                logger.debug("Generated {} UUIDs for new VanItems", generatedIdCount);
            }

            // Convert to JOOQ records for batch insert using proper JOOQ DSL
            List<com.salescode.dim.jooq.generated.tables.records.DmsVanItemsRecord> records = 
                newVanItems.stream()
                    .map(vanItem -> {
                        com.salescode.dim.jooq.generated.tables.records.DmsVanItemsRecord record =
                            dslContext.newRecord(DMS_VAN_ITEMS);
                        
                        // Map all common fields
                        record.setId(vanItem.getId());
                        record.setVersion(vanItem.getVersion() != null ? vanItem.getVersion() : INITIAL_VERSION);
                        record.setActiveStatus(vanItem.getActiveStatus());
                        record.setCreationTime(vanItem.getCreationTime());
                        record.setLastModifiedTime(vanItem.getLastModifiedTime());
                        record.setCreatedBy(vanItem.getCreatedBy());
                        record.setModifiedBy(vanItem.getModifiedBy());
                        
                        // Map ALL DMS-specific fields from VanItems
                        record.setBatchCode(vanItem.getBatchCode());
                        record.setSkuCode(vanItem.getSkuCode());
                        record.setBatchId(vanItem.getBatchId());
                        record.setItemType(vanItem.getItemType());
                        record.setLoadNumber(vanItem.getLoadNumber());
                        record.setMrp(vanItem.getMrp());
                        record.setBasePrice(vanItem.getBasePrice());
                        record.setCaseQty(vanItem.getCaseQty());
                        record.setPieceQty(vanItem.getPieceQty());
                        record.setOtherQty(vanItem.getOtherQty());
                        record.setCaseQtyLeft(vanItem.getCaseQtyLeft());
                        record.setPieceQtyLeft(vanItem.getPieceQtyLeft());
                        record.setOtherQtyLeft(vanItem.getOtherQtyLeft());
                        record.setSuggestedPieceQty(vanItem.getSuggestedPieceQty());
                        record.setSuggestedCaseQty(vanItem.getSuggestedCaseQty());
                        record.setSuggestedOtherQty(vanItem.getSuggestedOtherQty());
                        record.setAcceptedPieceQty(vanItem.getAcceptedPieceQty());
                        record.setAcceptedCaseQty(vanItem.getAcceptedCaseQty());
                        record.setAcceptedOtherQty(vanItem.getAcceptedOtherQty());
                        
                        return record;
                    })
                    .collect(Collectors.toList());

            // Execute batch insert using JOOQ batch API
            int[] results = dslContext.batchInsert(records).execute();
            
            int successCount = 0;
            for (int result : results) {
                if (result > 0) successCount++;
            }

            logger.info("Successfully batch inserted {} new VanItems ({} records affected)", newVanItems.size(), successCount);

        } catch (org.jooq.exception.DataAccessException e) {
            throw new LoadoutBatchSaveException(
                "Batch insert failed for VanItems due to database error: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "new VanItems count=" + newVanItems.size(),
                e
            );
        } catch (Exception e) {
            throw new LoadoutBatchSaveException(
                "Batch insert failed for VanItems: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "new VanItems count=" + newVanItems.size(),
                e
            );
        }
    }

    /**
     * Performs batch update operation for existing VanItems entities with version increment.
     * Executes JOOQ batch update for all existing VanItems records.
     * 
     * @param existingVanItems collection of existing VanItems entities to update
     * @throws LoadoutBatchSaveException if batch update fails
     *
     */
    public void batchUpdateVanItems(Collection<VanItems> existingVanItems) {
        if (existingVanItems == null || existingVanItems.isEmpty()) {
            logger.debug("No existing VanItems to update, skipping batch update");
            return;
        }

        logger.debug("Preparing to batch update {} existing VanItems", existingVanItems.size());
        
        try {
            // Convert to JOOQ update queries using proper JOOQ DSL
            List<org.jooq.Query> updateQueries = existingVanItems.stream()
                .map(vanItem -> {
                    return dslContext
                        .update(DMS_VAN_ITEMS)
                        // Update common fields
                        .set(DMS_VAN_ITEMS.VERSION, vanItem.getVersion())
                        .set(DMS_VAN_ITEMS.ACTIVE_STATUS, vanItem.getActiveStatus())
                        .set(DMS_VAN_ITEMS.LAST_MODIFIED_TIME, vanItem.getLastModifiedTime())
                        .set(DMS_VAN_ITEMS.MODIFIED_BY, vanItem.getModifiedBy())
                        // Update ALL DMS-specific fields
                        .set(DMS_VAN_ITEMS.BATCH_CODE, vanItem.getBatchCode())
                        .set(DMS_VAN_ITEMS.SKU_CODE, vanItem.getSkuCode())
                        .set(DMS_VAN_ITEMS.BATCH_ID, vanItem.getBatchId())
                        .set(DMS_VAN_ITEMS.ITEM_TYPE, vanItem.getItemType())
                        .set(DMS_VAN_ITEMS.LOAD_NUMBER, vanItem.getLoadNumber())
                        .set(DMS_VAN_ITEMS.MRP, vanItem.getMrp())
                        .set(DMS_VAN_ITEMS.BASE_PRICE, vanItem.getBasePrice())
                        .set(DMS_VAN_ITEMS.CASE_QTY, vanItem.getCaseQty())
                        .set(DMS_VAN_ITEMS.PIECE_QTY, vanItem.getPieceQty())
                        .set(DMS_VAN_ITEMS.OTHER_QTY, vanItem.getOtherQty())
                        .set(DMS_VAN_ITEMS.CASE_QTY_LEFT, vanItem.getCaseQtyLeft())
                        .set(DMS_VAN_ITEMS.PIECE_QTY_LEFT, vanItem.getPieceQtyLeft())
                        .set(DMS_VAN_ITEMS.OTHER_QTY_LEFT, vanItem.getOtherQtyLeft())
                        .set(DMS_VAN_ITEMS.SUGGESTED_PIECE_QTY, vanItem.getSuggestedPieceQty())
                        .set(DMS_VAN_ITEMS.SUGGESTED_CASE_QTY, vanItem.getSuggestedCaseQty())
                        .set(DMS_VAN_ITEMS.SUGGESTED_OTHER_QTY, vanItem.getSuggestedOtherQty())
                        .set(DMS_VAN_ITEMS.ACCEPTED_PIECE_QTY, vanItem.getAcceptedPieceQty())
                        .set(DMS_VAN_ITEMS.ACCEPTED_CASE_QTY, vanItem.getAcceptedCaseQty())
                        .set(DMS_VAN_ITEMS.ACCEPTED_OTHER_QTY, vanItem.getAcceptedOtherQty())
                        .where(DMS_VAN_ITEMS.ID.eq(vanItem.getId()));
                })
                .collect(Collectors.toList());

            // Execute batch update using JOOQ batch API
            int[] results = dslContext.batch(updateQueries).execute();
            
            int successCount = 0;
            for (int result : results) {
                if (result > 0) successCount++;
            }

            logger.info("Successfully batch updated {} existing VanItems ({} records affected)", existingVanItems.size(), successCount);

        } catch (org.jooq.exception.DataAccessException e) {
            throw new LoadoutBatchSaveException(
                "Batch update failed for VanItems due to database error: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "existing VanItems count=" + existingVanItems.size(),
                e
            );
        } catch (Exception e) {
            throw new LoadoutBatchSaveException(
                "Batch update failed for VanItems: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "existing VanItems count=" + existingVanItems.size(),
                e
            );
        }
    }

    /**
     * Main method to perform batch upsert operations for VanItems entities.
     * Orchestrates the complete upsert process: validating required fields,
     * querying for existing records by composite key, separating new from existing,
     * and executing batch insert and update operations.
     * 
     * @param vanItems collection of VanItems entities to upsert
     * @throws LoadoutBatchSaveException if validation or any operation fails
     *
     */
    public void batchUpsertVanItems(Collection<VanItems> vanItems) {
        if (vanItems == null || vanItems.isEmpty()) {
            return;
        }

        try {
            // Validate required fields for all items
            validationService.validateVanItemsCollection(vanItems);

            // Query existing VanItems by composite key
            Map<String, VanItems> existingVanItemsMap = queryExistingVanItemsByCompositeKey(vanItems);

            // Separate into new and existing VanItems
            List<VanItems> newVanItems = new ArrayList<>();
            List<VanItems> existingVanItems = new ArrayList<>();

            for (VanItems vanItem : vanItems) {
                String compositeKey = hierarchyService.createVanItemsCompositeKey(vanItem);
                if (existingVanItemsMap.containsKey(compositeKey)) {
                    // Update existing VanItems with new data while preserving database fields
                    VanItems existingVanItem = existingVanItemsMap.get(compositeKey);
                    AbstractCDMService.fillAttributes(existingVanItem, vanItem); // Copy non-null fields from input to existing
                    
                    // Fill common attributes and increment version for existing entities
                    fillVanItemsCommonAttributes(existingVanItem);
                    Integer currentVersion = existingVanItem.getVersion();
                    existingVanItem.setVersion(currentVersion != null ? currentVersion + 1 : INITIAL_VERSION);
                    
                    existingVanItems.add(existingVanItem);
                } else {
                    // Fill common attributes for new entities
                    fillVanItemsCommonAttributes(vanItem);
                    if (vanItem.getVersion() == null) {
                        vanItem.setVersion(INITIAL_VERSION);
                    }
                    newVanItems.add(vanItem);
                }
            }

            // Perform batch operations
            batchInsertVanItems(newVanItems);
            batchUpdateVanItems(existingVanItems);

            logger.info("Completed batch upsert for {} van items ({} new, {} updated)", 
                       vanItems.size(), newVanItems.size(), existingVanItems.size());

        } catch (LoadoutBatchSaveException e) {
            // Just rethrow - exception already contains context
            throw e;
        } catch (Exception e) {
            throw new LoadoutBatchSaveException(
                "Batch upsert failed for van items: " + e.getMessage(),
                ErrorType.DATABASE_ERROR,
                "van items count=" + vanItems.size(),
                e
            );
        }
    }
}
