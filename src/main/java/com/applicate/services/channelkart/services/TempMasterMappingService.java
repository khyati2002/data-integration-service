package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.repository.TempMasterMappingRepository;
import com.salescode.dim.jooq.impl.TempMasterMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_TEMP_MASTER_MAPPING;

public class TempMasterMappingService extends AbstractCDMService<TempMasterMapping> {


    private static final Logger LOG = LoggerFactory.getLogger(TempMasterMappingService.class);
    private static TempMasterMappingRepository tempMasterMappingRepository;

    public TempMasterMappingService() {
        if (tempMasterMappingRepository == null) {
            tempMasterMappingRepository = new TempMasterMappingRepository(getDslContext());
        }
    }

    public List<String> getActiveParentFromUserAndFeature(String userLoginId, String featureName) {
        return tempMasterMappingRepository.findByUserLoginIdAndFeature(userLoginId, featureName).stream()
                .filter(record -> record.getActiveStatus() != null && record.getActiveStatus() == ActiveStatus.ACTIVE)
                .map(TempMasterMapping::getParent)
                .collect(Collectors.toList());
    }

}
