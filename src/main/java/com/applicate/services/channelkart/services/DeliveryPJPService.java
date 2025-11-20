package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.jooq.generated.tables.pojos.DeliveryPjp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_DELIVERY_PJP;

public class DeliveryPJPService extends AbstractCDMService<DeliveryPjp> {
    private static final Logger LOG = LoggerFactory.getLogger(DeliveryPJPService.class);

    public List<List<DeliveryPjp>> getItemsToSaveList(List<DeliveryPjp> deliveryPjpList) {
        List<List<DeliveryPjp>> result = new ArrayList<>();
        List<String> pjpIds = deliveryPjpList.stream().map(DeliveryPjp::getId).collect(Collectors.toList());

        Map<String, DeliveryPjp> savedList = getDslContext()
                                                     .selectFrom(CK_DELIVERY_PJP)
                                                     .where(CK_DELIVERY_PJP.ID.in(pjpIds))
                                                     .fetch()
                                                     .intoMap(CK_DELIVERY_PJP.ID, record -> record.into(DeliveryPjp.class));

        List<DeliveryPjp> itemsToInsert = new ArrayList<>();
        List<DeliveryPjp> itemsToUpdate = new ArrayList<>();

        for (DeliveryPjp pjp : deliveryPjpList) {
            fillAttributes(pjp, savedList.get(pjp.getId()));
            fillCommonAttributes(pjp);

            if (savedList.get(pjp.getId()) == null) {
                pjp.setVersion(0);
                itemsToInsert.add(pjp);
                pjp.setOperationPerformed(ActionType.INSERT);
            } else {
                DeliveryPjp existingPjp = savedList.get(pjp.getId());
                pjp.setVersion(existingPjp.getVersion() + 1);
                pjp.setOperationPerformed(ActionType.UPDATE);
                itemsToUpdate.add(pjp);
            }
        }

        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }

    @Override
    public Collection<DeliveryPjp> batchSave(Collection<DeliveryPjp> deliveryPjpList) {
        LOG.info("Size of DeliveryPJP list is " + deliveryPjpList.size());
        List<DeliveryPjp> deliveryPjps = new ArrayList<>(deliveryPjpList);
        List<List<DeliveryPjp>> saveItemsList = getItemsToSaveList(deliveryPjps);

        // Set attributes for items to insert
        saveItemsList.get(0).forEach(pjp -> {
            pjp.setActiveStatus(ActiveStatus.ACTIVE);
            pjp.setChanged(true);
        });


        saveItemsList.get(1).forEach(pjp -> {
            pjp.setActiveStatus(ActiveStatus.ACTIVE);
            pjp.setChanged(true);
        });


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
                                return getDslContext().newRecord(CK_DELIVERY_PJP, pjp);
                            })
                            .collect(Collectors.toList())
            ).execute();
        }

        LOG.info("DeliveryPJP batch save successful");
        return deliveryPjps;
    }

    public String getLoginIdByOutletcode(String outletcode){
        return getDslContext().select(CK_DELIVERY_PJP.LOGINID)
                .from(CK_DELIVERY_PJP)
                .where(CK_DELIVERY_PJP.OUTLETCODE.eq(outletcode))
                .and(CK_DELIVERY_PJP.PJP_DATE.cast(LocalDate.class).eq(LocalDate.now()))
                .orderBy(CK_DELIVERY_PJP.CREATION_TIME.desc())
                .limit(1)
                .fetchOne(CK_DELIVERY_PJP.LOGINID);
    }

}