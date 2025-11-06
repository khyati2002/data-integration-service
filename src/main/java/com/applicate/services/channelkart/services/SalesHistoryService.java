package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.impl.SalesHistory;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.salescode.dim.jooq.generated.tables.records.CkSalesHistoryRecord;
import com.salescode.dim.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.*;

public class SalesHistoryService extends AbstractCDMService<SalesHistory> {
    private static final Logger LOG = LoggerFactory.getLogger(SalesHistoryService.class);

    public SalesHistoryService() {
    }

    public List<List<SalesHistory>> getItemsToSaveList(List<SalesHistory> salesHistoryList) {
        List<List<SalesHistory>> result = new ArrayList<>();

        // Collect all IDs from the input list
        List<String> ids = salesHistoryList.stream()
                .filter(history -> history.getId() != null)
                .map(SalesHistory::getId)
                .collect(Collectors.toList());

        // Fetch existing records from database
        Map<String, SalesHistory> savedList = new HashMap<>();
        if (!ids.isEmpty()) {
            savedList = getDslContext()
                    .select(CK_SALES_HISTORY.asterisk())
                    .from(CK_SALES_HISTORY)
                    .where(CK_SALES_HISTORY.ID.in(ids))
                    .fetch()
                    .intoMap(CK_SALES_HISTORY.ID,
                            record -> record.into(SalesHistory.class));
        }

        List<SalesHistory> itemsToInsert = new ArrayList<>();
        List<SalesHistory> itemsToUpdate = new ArrayList<>();

        for (SalesHistory history : salesHistoryList) {
            fillAttributes(history, savedList.get(history.getId()));
            fillCommonAttributes(history);
            new AttributeUpdateOverrideManager().overrideAttributes(history, savedList.get(history.getId()));

            super.addHash(history);

            if (history.getId() == null || savedList.get(history.getId()) == null) {
                // New record to insert
                history.setVersion(0);
                if (history.getId() == null) {
                    history.setId(UUID.randomUUID().toString());
                }
                history.setChanged(true);
                itemsToInsert.add(history);
                history.setOperationPerformed(ActionType.INSERT);
            } else {
                // Existing record to potentially update
                SalesHistory existingHistory = savedList.get(history.getId());
                history.setVersion(existingHistory.getVersion() + 1);

                if (!Objects.equals(history.getHash(), existingHistory.getHash())) {
                    history.setChanges(CdmDiffUtil.getChanges(history, existingHistory));
                    history.setOperationPerformed(ActionType.UPDATE);
                    history.setChanged(true);
                    itemsToUpdate.add(history);
                }
            }
        }

        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }

    @Override
    public Collection<SalesHistory> batchSave(Collection<SalesHistory> salesHistoryCollection) {
        LOG.info("Size of list is " + salesHistoryCollection.size());
        List<SalesHistory> salesHistoryList = new ArrayList<>(salesHistoryCollection);

        List<List<SalesHistory>> saveItemsList = getItemsToSaveList(salesHistoryList);

        // Batch insert new records
        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(
                    saveItemsList.get(0).stream()
                            .map(history -> getDslContext().newRecord(CK_SALES_HISTORY, history))
                            .collect(Collectors.toList())
            ).execute();
            LOG.info("Inserted {} sales history records", saveItemsList.get(0).size());
        }

        // Batch update existing records
        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(
                    saveItemsList.get(1).stream()
                            .map(history -> {
                                CkSalesHistoryRecord record = getDslContext().newRecord(CK_SALES_HISTORY, history);
                                return record;
                            })
                            .collect(Collectors.toList())
            ).execute();
            LOG.info("Updated {} sales history records", saveItemsList.get(1).size());
        }

        LOG.info("Batch save successful for sales history");
        CacheManager.getInstance().evictAll("dataintegration-sales-history");
        return salesHistoryList;
    }
}