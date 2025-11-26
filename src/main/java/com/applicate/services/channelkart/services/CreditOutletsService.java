package com.applicate.services.channelkart.services;


import com.applicate.services.channelkart.models.enums.ActionType;
import com.esotericsoftware.minlog.Log;
import com.salescode.dim.jooq.impl.CreditOutlets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.tables.CreditOutlets.CREDIT_OUTLETS;


public class CreditOutletsService extends AbstractCDMService<CreditOutlets> {

    private static final Logger LOG = LoggerFactory.getLogger(CreditOutletsService.class);


    @Override
    public Collection<CreditOutlets> batchSave(Collection<CreditOutlets> creditOutletsList){
        try{
            LOG.info("Size of list is {}", creditOutletsList.size());

            List<CreditOutlets> creditOutlets = new ArrayList<>(creditOutletsList);
            List<List<CreditOutlets>> saveItemsList = getItemsToSaveList(creditOutlets);

            List<CreditOutlets> inserts=saveItemsList.get(0);
            List<CreditOutlets> updates=saveItemsList.get(1);

            LOG.info("items to be added: {}", inserts.size());
            LOG.info("items to be updated: {}", updates.size());

            if (!inserts.isEmpty()) {
                getDslContext().batchInsert(
                        inserts.stream()
                                .map(creditOutlet -> getDslContext().newRecord(CREDIT_OUTLETS, creditOutlet))
                                .collect(Collectors.toList())
                ).execute();
            }
            if (!updates.isEmpty()) {
                updates.removeIf(creditOutlet ->{
                    if(creditOutlet.getId()==null){
                        LOG.warn("Skipping update for {} because ID is missing", creditOutlet.getOutletCode());
                        return true;
                    }
                    return false;
                });
                getDslContext().batchUpdate(
                        updates.stream()
                                .map(creditOutlet -> getDslContext().newRecord(CREDIT_OUTLETS, creditOutlet))
                                .collect(Collectors.toList())
                ).execute();
            }

            LOG.info("Batch save successful");
             return creditOutletsList;
        } catch (Exception e) {
            Log.error(e.getMessage());
            throw new RuntimeException(e);
        }

    }

    public List<List<CreditOutlets>> getItemsToSaveList(List<CreditOutlets> creditOutletsList){
        List<List<CreditOutlets>> result = new ArrayList<>();
        List<String> creditOutletCodes = creditOutletsList.stream()
                .map(CreditOutlets::getOutletCode)
                .collect(Collectors.toList());

        Map<String, CreditOutlets> savedList = getDslContext()
                .select(CREDIT_OUTLETS.asterisk())
                .from(CREDIT_OUTLETS)
                .where(CREDIT_OUTLETS.OUTLET_CODE.in(creditOutletCodes))
                .fetch()
                .intoMap(CREDIT_OUTLETS.OUTLET_CODE, CreditOutlets.class );

        List<CreditOutlets> itemsToInsert = new ArrayList<>();
        List<CreditOutlets> itemsToUpdate = new ArrayList<>();

        for (CreditOutlets creditOutlet : creditOutletsList) {
            CreditOutlets existingOutlet = savedList.get(creditOutlet.getOutletCode());

            if (existingOutlet == null) {
                creditOutlet.setVersion(0);
                fillCommonAttributes(creditOutlet);
                creditOutlet.setOperationPerformed(ActionType.INSERT);

                itemsToInsert.add(creditOutlet);
            } else {
                fillAttributes(creditOutlet,existingOutlet);
                fillCommonAttributes(creditOutlet);
                creditOutlet.setId(existingOutlet.getId());
                creditOutlet.setVersion(existingOutlet.getVersion() + 1);
                creditOutlet.setOperationPerformed(ActionType.UPDATE);

                itemsToUpdate.add(creditOutlet);

            }
        }
        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }
}
