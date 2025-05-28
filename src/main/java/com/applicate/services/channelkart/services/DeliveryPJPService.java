package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.JSONUtils;

//import com.salescode.dim.jooq.generated.tables.pojos.DeliveryPjp;
import com.salescode.dim.jooq.impl.DeliveryPJP;
import com.salescode.dim.jooq.generated.tables.records.CkDeliveryPjpRecord;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.*;

public class DeliveryPJPService extends AbstractCDMService<DeliveryPJP> {
    private static final String DAY_TAG = "day";
    private static final String FREQUENCY_TAG = "frequency";

    private static final Logger LOG = LoggerFactory.getLogger(DeliveryPJPService.class);

    public void addDayAndFrequency(DeliveryPJP pjp) {

        ObjectNode dayAndFrequency = JSONUtils.getObjectMapper().createObjectNode();

        LocalDateTime pjpDate = pjp.getPjpDate();

        Date date = Date.from(pjpDate.atZone(ZoneId.systemDefault()).toInstant());

        Calendar currDate = Calendar.getInstance();
        currDate.setTime(date);

        dayAndFrequency.put(DAY_TAG,
                currDate.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH).toLowerCase());
        dayAndFrequency.put(FREQUENCY_TAG, currDate.get(Calendar.WEEK_OF_MONTH));
        pjp.setDayAndFrequency(JSONUtils.getObjectMapper().createArrayNode().add(dayAndFrequency));
        pjp.setMonth(currDate.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH).toLowerCase());
        pjp.setYear(currDate.get(Calendar.YEAR) + "");
    }


    public List<List<DeliveryPJP>> getItemsToSaveList(List<DeliveryPJP> deliveryPjpList){
        List<List<DeliveryPJP>> result = new ArrayList<>();
        List<String> ids = deliveryPjpList.stream()
                .map(DeliveryPJP::getId)
                .collect(Collectors.toList());

        Map<String, com.salescode.dim.jooq.generated.tables.pojos.DeliveryPjp> savedList = getDslContext()
                .selectFrom(CK_DELIVERY_PJP)
                .where(CK_DELIVERY_PJP.ID.in(ids))
                .fetch()
                .intoMap(CK_DELIVERY_PJP.ID, record ->  record.into(com.salescode.dim.jooq.generated.tables.pojos.DeliveryPjp.class));
        List<DeliveryPJP> itemsToInsert = new ArrayList<>();
        List<DeliveryPJP> itemsToUpdate = new ArrayList<>();
        for (DeliveryPJP pjp : deliveryPjpList) {
            fillAttributes(pjp, DeliveryPJP.of(savedList.get(pjp.getId())));
            fillCommonAttributes(pjp);
            if (savedList.get(pjp.getId()) == null) {
                pjp.setVersion(0);
                if(pjp.getId() == null) {
                    pjp.setId(UUID.randomUUID().toString());
                }
                pjp.setChanged((byte) 1);
                itemsToInsert.add(pjp);
                pjp.setOperationPerformed(ActionType.INSERT);
            } else {
                DeliveryPJP existingPJP = DeliveryPJP.of(savedList.get(pjp.getId()));
                pjp.setId(existingPJP.getId());
                pjp.setVersion(existingPJP.getVersion() + 1);
//                String pjphash = pjp.getHash();
//                String existingHash = existingPJP.getHash();
//                if (!Objects.equals(pjp.getHash(), existingPJP.getHash())) {
//                    pjp.setChanges(CdmDiffUtil.getChanges(pjp,existingPJP));
//                    pjp.setOperationPerformed(ActionType.UPDATE);
//                    pjp.setChanged((byte) 1);
//
                   itemsToUpdate.add(pjp);
//                }
            }
        }
        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }



    public Collection<DeliveryPJP> batchSave(Collection<DeliveryPJP> deliveryPJPList){
        LOG.info("Size of list is "  + deliveryPJPList.size());
        List<DeliveryPJP> deliverypjplist = new ArrayList<>(deliveryPJPList);
        for (DeliveryPJP pjp : deliverypjplist) {
            pjp.setSequence(0);
        }
        LOG.info("Pre Batch Save Called with size " + deliverypjplist.size());
        List<List<DeliveryPJP>> saveItemsList = getItemsToSaveList(deliverypjplist);
        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(
                    saveItemsList.get(0).stream()
                            .map(pjp -> getDslContext().newRecord(CK_DELIVERY_PJP, pjp))
                            .collect(Collectors.toList())
            ).execute();
        }
        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(
                    saveItemsList.get(1).stream()
                            .map(pjp -> {
                                CkDeliveryPjpRecord record = getDslContext().newRecord(CK_DELIVERY_PJP, pjp);
                                record.changed(CK_DELIVERY_PJP.ID, false); // Avoid updating primary key
                                return record;
                            })
                            .collect(Collectors.toList())
            ).execute();
        }
        LOG.info("Batch save successful");
        return deliverypjplist;
    }

}
