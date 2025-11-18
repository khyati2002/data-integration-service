package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.repository.TempMasterMappingRepository;
import com.salescode.dim.jooq.impl.TempMasterMapping;

import java.util.List;
import java.util.stream.Collectors;


public class TempMasterMappingService extends AbstractCDMService<TempMasterMapping> {

    private  final TempMasterMappingRepository tempMasterMappingRepository;

    public TempMasterMappingService(TempMasterMappingRepository repository) {
        super();
        this.tempMasterMappingRepository = repository;
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

}