package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.jooq.impl.GRNInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static com.salescode.dim.jooq.generated.tables.CkGrnInfo.CK_GRN_INFO;

public class SalesGRNService extends AbstractCDMService<GRNInfo>{

    private static final Logger LOG = LoggerFactory.getLogger(SalesGRNService.class);


    public void addNewEntry(GRNInfo newGrnInfo) {
        super.save(newGrnInfo);
    }

    @Override
    public Collection<GRNInfo> batchSave(Collection<GRNInfo> grnInfoList) {
        LOG.info("Size of Grn list is {}" , grnInfoList.size());
        List<GRNInfo> grnInfos = new ArrayList<>(grnInfoList);
        List<List<GRNInfo>> saveItemsList = getItemsToSaveList(grnInfos);

        // Set attributes for items to insert
        saveItemsList.get(0).forEach(info -> {
            info.setActiveStatus(ActiveStatus.ACTIVE);
            info.setChanged(true);
        });
        
        saveItemsList.get(1).forEach(info -> {
            info.setActiveStatus(ActiveStatus.ACTIVE);
            info.setChanged(true);
        });


        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(
                    saveItemsList.get(0).stream()
                            .map(info-> getDslContext().newRecord(CK_GRN_INFO, info))
                            .collect(Collectors.toList())
            ).execute();
        }

        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(
                    saveItemsList.get(1).stream()
                            .map(info -> {
                                return getDslContext().newRecord(CK_GRN_INFO, info);
                            })
                            .collect(Collectors.toList())
            ).execute();
        }

        LOG.info("GRN info batch save successful");
        return grnInfos;
    }

    public List<List<GRNInfo>> getItemsToSaveList(List<GRNInfo> grnInfoList) {
        List<List<GRNInfo>> result = new ArrayList<>();
        List<String> grnInfoIds = grnInfoList.stream().map(GRNInfo::getId).collect(Collectors.toList());

        Map<String, GRNInfo> savedList = getDslContext()
                                                     .selectFrom(CK_GRN_INFO)
                                                     .where(CK_GRN_INFO.ID.in(grnInfoIds))
                                                     .fetch()
                                                     .intoMap(CK_GRN_INFO.ID, rec -> rec.into(GRNInfo.class));

        List<GRNInfo> itemsToInsert = new ArrayList<>();
        List<GRNInfo> itemsToUpdate = new ArrayList<>();

        for (GRNInfo grnInfo : grnInfoList) {
            fillAttributes(grnInfo, savedList.get(grnInfo.getId()));
            fillCommonAttributes(grnInfo);

            if (savedList.get(grnInfo.getId()) == null) {
                grnInfo.setId(new IdGenerator(grnInfo.getClass().getSimpleName()).getId(grnInfo));
                grnInfo.setVersion(0);
                itemsToInsert.add(grnInfo);
                grnInfo.setOperationPerformed(ActionType.INSERT);
            } else {
                GRNInfo existinggrnInfo = savedList.get(grnInfo.getId());
                grnInfo.setVersion(existinggrnInfo.getVersion() + 1);
                grnInfo.setOperationPerformed(ActionType.UPDATE);
                itemsToUpdate.add(grnInfo);
            }
        }

        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }
    
    
    

}
