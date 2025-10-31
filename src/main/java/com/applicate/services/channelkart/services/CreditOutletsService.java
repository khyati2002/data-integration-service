package com.applicate.services.channelkart.services;


import com.esotericsoftware.minlog.Log;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.jooq.generated.tables.records.CkOutletDetailsRecord;
import com.salescode.dim.jooq.generated.tables.records.CreditOutletsRecord;
import com.salescode.dim.jooq.impl.CreditOutlets;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.tables.CreditOutlets.CREDIT_OUTLETS;


public class CreditOutletsService extends AbstractCDMService<CreditOutlets> {

    private static final Logger LOG = LoggerFactory.getLogger(CreditOutletsService.class);


    @Override
    public Collection<CreditOutlets> batchSave(Collection<CreditOutlets> creditOutletsList){
        try{
            LOG.info("Size of list is "  + creditOutletsList.size());
            if (creditOutletsList != null && !creditOutletsList.isEmpty()) {
                creditOutletsList.forEach(creditOutlet -> {
                    if (creditOutlet.getId() == null || creditOutlet.getId().isEmpty()) {
                        creditOutlet.setId(UUID.randomUUID().toString());
                    }
                });
                List<CreditOutletsRecord> records = creditOutletsList.stream()
                        .map(creditOutlet -> getDslContext().newRecord(CREDIT_OUTLETS, creditOutlet))
                        .collect(Collectors.toList());

                getDslContext().batchInsert(records).execute();
            }

            LOG.info("Batch save successful");
//        CacheManager.getInstance().evictAll("dataintegration-outlets");
            return creditOutletsList;
        } catch (Exception e) {
            Log.error(e.getMessage());
            throw new RuntimeException(e);
        }

    }
}
