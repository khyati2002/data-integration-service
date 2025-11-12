package com.applicate.services.channelkart.services;


import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.esotericsoftware.minlog.Log;
import com.salescode.dim.jooq.generated.tables.records.CreditOutletsRecord;
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
            LOG.info("Size of list is "  + creditOutletsList.size());

            List<CreditOutlets> creditOutlets = new ArrayList<>(creditOutletsList);
            List<List<CreditOutlets>> saveItemsList = getItemsToSaveList(creditOutlets);
            if (!saveItemsList.get(0).isEmpty()) {
                getDslContext().batchInsert(
                        saveItemsList.get(0).stream()
                                .map(creditOutlet -> getDslContext().newRecord(CREDIT_OUTLETS, creditOutlet))
                                .collect(Collectors.toList())
                ).execute();
            }
            if (!saveItemsList.get(1).isEmpty()) {
                getDslContext().batchUpdate(
                        saveItemsList.get(1).stream()
                                .map(creditOutlet -> {
                                    CreditOutletsRecord record = getDslContext().newRecord(CREDIT_OUTLETS, creditOutlet);
                                    return record;
                                })
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
                .intoMap(CREDIT_OUTLETS.OUTLET_CODE, record -> record.into(CreditOutlets.class));
        List<CreditOutlets> itemsToInsert = new ArrayList<>();
        List<CreditOutlets> itemsToUpdate = new ArrayList<>();
        for (CreditOutlets creditOutlet : creditOutletsList) {
            fillAttributes(creditOutlet,CreditOutlets.of(savedList.get(creditOutlet.getOutletCode())));
            fillCommonAttributes(creditOutlet);
            new AttributeUpdateOverrideManager().overrideAttributes(creditOutlet,savedList.get(creditOutlet.getOutletCode()));

            super.addHash(creditOutlet);
            if (savedList.get(creditOutlet.getOutletCode()) == null) {
                creditOutlet.setVersion(0);
                creditOutlet.setChanged(true);
                itemsToInsert.add(creditOutlet);
                creditOutlet.setOperationPerformed(ActionType.INSERT);
            } else {
                CreditOutlets existingOutlet = CreditOutlets.of(savedList.get(creditOutlet.getOutletCode()));
                creditOutlet.setId(existingOutlet.getId());
                creditOutlet.setVersion(existingOutlet.getVersion() + 1);


                if (!Objects.equals(creditOutlet.getHash(), existingOutlet.getHash())) {
                    creditOutlet.setChanges(CdmDiffUtil.getChanges(creditOutlet,existingOutlet));
                    creditOutlet.setOperationPerformed(ActionType.UPDATE);
                    itemsToUpdate.add(creditOutlet);
                }
            }
        }
        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }
}
