package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.jooq.generated.tables.pojos.SalesHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static com.salescode.dim.jooq.generated.Tables.CK_SALES_HISTORY;

public class SalesHistoryService extends AbstractCDMService<SalesHistory> {
    private static final Logger LOG = LoggerFactory.getLogger(SalesHistoryService.class);

    private static final String GENERATED_TAG = "GENERATED";

    public List<List<SalesHistory>> getItemsToSaveList(List<SalesHistory> salesHistoryList) {
        List<List<SalesHistory>> result = new ArrayList<>();
        List<String> historyIds = salesHistoryList.stream()
                                          .map(SalesHistory::getId)
                                          .collect(Collectors.toList());

        Map<String, SalesHistory> savedList = getDslContext()
                                                      .selectFrom(CK_SALES_HISTORY)
                                                      .where(CK_SALES_HISTORY.ID.in(historyIds))
                                                      .fetch()
                                                      .intoMap(CK_SALES_HISTORY.ID, record -> record.into(SalesHistory.class));

        List<SalesHistory> itemsToInsert = new ArrayList<>();
        List<SalesHistory> itemsToUpdate = new ArrayList<>();

        for (SalesHistory history : salesHistoryList) {
            fillAttributes(history, savedList.get(history.getId()));
            fillCommonAttributes(history);

            if (history.getStatus() != null) {
                history.setStatus(history.getStatus().toUpperCase());
                if (!isValidStatus(history.getStatus())) {
                    throw new IllegalArgumentException("Status value NOT ALLOWED: " + history.getStatus());
                }
            }

            if (savedList.get(history.getId()) == null) {
                history.setVersion(0);

                if (history.getCreationTime() == null) {
                    history.setCreationTime(LocalDateTime.now());
                }

                itemsToInsert.add(history);
                history.setOperationPerformed(ActionType.INSERT);
            } else {
                SalesHistory existingHistory = savedList.get(history.getId());
                history.setVersion(existingHistory.getVersion() + 1);
                history.setOperationPerformed(ActionType.UPDATE);
                itemsToUpdate.add(history);
            }
        }

        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }

    @Override
    public Collection<SalesHistory> batchSave(Collection<SalesHistory> salesHistoryList) {
        LOG.info("Size of SalesHistory list is " + salesHistoryList.size());
        List<SalesHistory> salesHistories = new ArrayList<>(salesHistoryList);
        List<List<SalesHistory>> saveItemsList = getItemsToSaveList(salesHistories);

        saveItemsList.get(0).forEach(history -> {
            history.setActiveStatus(ActiveStatus.ACTIVE);
            history.setChanged(Boolean.TRUE);
        });

        saveItemsList.get(1).forEach(history -> {
            history.setActiveStatus(ActiveStatus.ACTIVE);
            history.setChanged(Boolean.TRUE);
        });

        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(
                    saveItemsList.get(0).stream()
                            .map(history -> getDslContext().newRecord(CK_SALES_HISTORY, history))
                            .collect(Collectors.toList())
            ).execute();
        }

        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(
                    saveItemsList.get(1).stream()
                            .map(history -> {
                                return getDslContext().newRecord(CK_SALES_HISTORY, history);
                            })
                            .collect(Collectors.toList())
            ).execute();
        }
        LOG.info("SalesHistory batch save successful");
        return salesHistories;
    }

    public boolean isValidStatus(String status) {
        if (status == null) {
            return false;
        }
        List<String> validStatuses = List.of(
                GENERATED_TAG, "CONFIRMED", "PENDING", "DELIVERED",
                "CANCELLED", "REJECTED", "PROCESSING", "SHIPPED"
        );
        return validStatuses.contains(status.toUpperCase());
    }

}