package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.jooq.generated.tables.pojos.DeliveryPjp;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_DELIVERY_PJP;


public class DeliveryPJPService extends AbstractCDMService<DeliveryPjp> {
    protected EntityUtils entityUtils;
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

    public DeliveryPjp findByOutletCodeAndLoginIdAndMonthAndYear(String outletCode, String loginId, String month, String year) {
        return getDslContext()
                .selectFrom(CK_DELIVERY_PJP)
                .where(CK_DELIVERY_PJP.OUTLETCODE.eq(outletCode))
                .and(CK_DELIVERY_PJP.LOGINID.eq(loginId))
                .and(CK_DELIVERY_PJP.MONTH.eq(month))
                .and(CK_DELIVERY_PJP.YEAR.eq(year))
                .fetchOneInto(DeliveryPjp.class);
    }

    public void addDayAndFrequency(DeliveryPjp pjp) {

        ObjectMapper objectMapper = JSONUtils.getObjectMapper();
        ObjectNode dayAndFrequency = objectMapper.createObjectNode();
        LocalDateTime pjpDate = pjp.getPjpDate();
        Date date = Date.from(pjpDate.atZone(ZoneId.systemDefault()).toInstant());
        Calendar currDate = Calendar.getInstance();
        currDate.setTime(date);

        dayAndFrequency.put("day",
                currDate.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH).toLowerCase());
        dayAndFrequency.put("frequency", currDate.get(Calendar.WEEK_OF_MONTH));

        ArrayNode dayAndFrequencyArray = objectMapper.createArrayNode();
        dayAndFrequencyArray.add(dayAndFrequency);

        pjp.setDayAndFrequency(org.jooq.JSON.valueOf(dayAndFrequencyArray.toString()));

        pjp.setMonth(currDate.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH).toLowerCase());
        pjp.setYear(currDate.get(Calendar.YEAR) + "");
    }

//    public DeliveryPjp refresh(DeliveryPjp cdmObject) {
//        // Fetch existing record from DB by primary key (id)
//        DeliveryPjp dbRecord = getDslContext()
//                .selectFrom(CK_DELIVERY_PJP)
//                .where(CK_DELIVERY_PJP.ID.eq(cdmObject.getId()))
//                .fetchOneInto(DeliveryPjp.class);
//
//        if (dbRecord != null) {
//            cdmObject.setOldModel(dbRecord.getOldModel());
//            int version = dbRecord.getVersion();
//
//            // Copy properties from input to dbRecord except version
//            EntityUtils.copyProperties(cdmObject, dbRecord);
//            org.apache.commons.beanutils.BeanUtils.copyProperties(cdmObject, dbRecord);
//
//
//            // Restore original DB version to dbRecord
//            dbRecord.setVersion(version);
//            return dbRecord;
//        } else {
//            // Mark input object as new record to create
//            cdmObject.setCreate(true);
//            return cdmObject;
//        }
//    }


}