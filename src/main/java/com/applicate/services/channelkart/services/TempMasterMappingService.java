package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.repository.TempMasterMappingRepository;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.jooq.generated.tables.records.CkGenericObjectRecord;
import com.salescode.dim.jooq.generated.tables.records.CkOutletDetailsRecord;
import com.salescode.dim.jooq.generated.tables.records.CkTempMasterMappingRecord;
import com.salescode.dim.jooq.impl.GenericEntity;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.TempMasterMapping;
import com.salescode.dim.jooq.impl.User;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.*;
import static com.salescode.dim.jooq.generated.Tables.CK_GENERIC_OBJECT;
import static org.apache.flink.optimizer.Optimizer.LOG;


public class TempMasterMappingService extends AbstractCDMService<TempMasterMapping> {

    private  final TempMasterMappingRepository tempMasterMappingRepository;

    public TempMasterMappingService() {
        super();
        this.tempMasterMappingRepository = new TempMasterMappingRepository(getDslContext());
    }

    public List<TempMasterMapping> findByLoginIdAndFeature(String userLoginId, String featureName) {
        return tempMasterMappingRepository.findByUserLoginIdAndFeature(userLoginId, featureName);
    }

    public TempMasterMapping findByLoginIdAndParentAndFeature(String userLoginId, String parent, String featureName) {
        return tempMasterMappingRepository.findByUserLoginIdAndParentAndFeature(userLoginId, parent, featureName);
    }

    public List<TempMasterMapping> findActiveLoginIdAndFeature(String userLoginId, String featureName) {
//        return tempMasterMappingRepository.findByUserLoginIdAndFeature(userLoginId, featureName).stream().filter(CommonDataModel::isActive).collect(Collectors.toList());
        return tempMasterMappingRepository.findByUserLoginIdAndFeatureAndActive(userLoginId, featureName, ActiveStatus.ACTIVE);
    }

    public List<String> getActiveParentFromUserAndFeature(String userLoginId, String featureName) {
//        return tempMasterMappingRepository.findByUserLoginIdAndFeature(userLoginId, featureName).stream().filter(CommonDataModel::isActive).map(TempMasterMapping::getParent).collect(Collectors.toList());
        List<TempMasterMapping> activeMappings = tempMasterMappingRepository.findByUserLoginIdAndFeatureAndActive(userLoginId, featureName, ActiveStatus.ACTIVE);
        return   activeMappings.stream()
                .map(TempMasterMapping::getParent)
                .collect(Collectors.toList());
    }

    public List<TempMasterMapping> getAllActiveRecordsByFeature(String featureName) {
        return tempMasterMappingRepository.findByFeatureAndActiveStatus(featureName, ActiveStatus.ACTIVE);
    }

    public TempMasterMapping refreshUsingJooq(TempMasterMapping cdmObject) {
        return tempMasterMappingRepository.refreshUsingJooq(cdmObject);
    }

    @Override
    public Collection<TempMasterMapping> batchSave(Collection<TempMasterMapping> tempMasterList) {
        LOG.info("Size of list is " + tempMasterList.size());
        List<TempMasterMapping> tempMasterMapping = new ArrayList<>(tempMasterList);
        List<List<TempMasterMapping>> saveItemsList = getItemsToSaveList(tempMasterMapping);
        saveItemsList.get(0).forEach(masterMapping -> {
            masterMapping.setActiveStatus(ActiveStatus.ACTIVE);
//            masterMapping.setRangeKey(0L);
//            masterMapping.setTimestamp(new Date().toInstant().toEpochMilli());
            masterMapping.setChanged(Boolean.TRUE);
        });

        saveItemsList.get(1).forEach(masterMapping -> {
            masterMapping.setActiveStatus(ActiveStatus.ACTIVE);
//            masterMapping.setRangeKey(0L);
//            masterMapping.setTimestamp(new Date().toInstant().toEpochMilli());
            masterMapping.setChanged(Boolean.TRUE);
        });
        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(saveItemsList.get(0).stream().map(loginId -> getDslContext().newRecord(CK_TEMP_MASTER_MAPPING, loginId)).collect(Collectors.toList())).execute();
        }
        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(saveItemsList.get(1).stream().map(loginId -> {
                CkTempMasterMappingRecord record = getDslContext().newRecord(CK_TEMP_MASTER_MAPPING, loginId);
                return record;
            }).collect(Collectors.toList())).execute();
        }

        LOG.info("Batch save successful");
        return tempMasterMapping;
    }

    public List<List<TempMasterMapping>> getItemsToSaveList(List<TempMasterMapping> tempMasterList) {
        List<List<TempMasterMapping>> result = new ArrayList<>();

        List<String> ids = tempMasterList.stream()
                .map(t -> t.getExtendedAttributes().get("OutletCode").toString().replace("\"", "") + "-" + t.getUserloginid() + "-" + t.getParent() + "-" + t.getFeature())
                .collect(Collectors.toList());

        Map<String, TempMasterMapping> savedList = getDslContext().selectFrom(CK_TEMP_MASTER_MAPPING).where(CK_TEMP_MASTER_MAPPING.ID.in(ids)).fetch().intoMap(CK_TEMP_MASTER_MAPPING.ID, record -> convertToTempMasterMapping(record));

        List<TempMasterMapping> itemsToInsert = new ArrayList<>();
        List<TempMasterMapping> itemsToUpdate = new ArrayList<>();
        for (TempMasterMapping masterMapping : tempMasterList) {
            fillAttributes(masterMapping, savedList.get(masterMapping.getId()));
            fillCommonAttributes(masterMapping);
            if (masterMapping.getId() == null) {
//                masterMapping.setId(new IdGenerator(masterMapping.getClass().getSimpleName()).getId(masterMapping));

                String id = masterMapping.getExtendedAttributes().get("OutletCode").toString().replace("\"", "")
                        + "-" + masterMapping.getUserloginid()
                        + "-" + masterMapping.getParent()
                        + "-" + masterMapping.getFeature();
                masterMapping.setId(id);
            }

            if (savedList.get(masterMapping.getId()) == null) {
                itemsToInsert.add(masterMapping);
//                loginId.setRangeKey(0L);
//                loginId.setTimestamp(new Date().toInstant().toEpochMilli());
                masterMapping.setOperationPerformed(ActionType.INSERT);
            } else {
                TempMasterMapping existingOutlet = savedList.get(masterMapping.getId());
                masterMapping.setOperationPerformed(ActionType.UPDATE);
//                loginId.setRangeKey(0L);
                masterMapping.setChanged(true);
//                loginId.setTimestamp(new Date().toInstant().toEpochMilli());
                itemsToUpdate.add(masterMapping);
            }
        }
        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }

    private TempMasterMapping convertToTempMasterMapping(CkTempMasterMappingRecord record) {
        TempMasterMapping entity = new TempMasterMapping();
        entity.setId(record.getId());
        entity.setUserloginid(record.getUserloginid());
        entity.setFeature(record.getFeature());
        entity.setParent(record.getParent());
        entity.setChanged(true);
        entity.setActiveStatus(record.getActiveStatus());
        entity.setExtendedAttributes((record.getExtendedAttributes()));
        return entity;
    }


}
