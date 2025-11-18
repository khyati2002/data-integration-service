package com.applicate.services.channelkart.repository;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.jooq.impl.TempMasterMapping;
import org.jooq.DSLContext;

import static com.salescode.dim.jooq.generated.Tables.CK_TEMP_MASTER_MAPPING;

import java.util.List;


public class TempMasterMappingRepository {

    private final DSLContext dsl;

    public TempMasterMappingRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<TempMasterMapping> findByUserLoginIdAndFeature(String userLoginId, String feature){
        return   dsl
                .selectFrom(CK_TEMP_MASTER_MAPPING)
                .where(CK_TEMP_MASTER_MAPPING.USERLOGINID.eq(userLoginId))
                .and(CK_TEMP_MASTER_MAPPING.FEATURE.eq(feature))
                .fetchInto(TempMasterMapping.class);

    }

    public TempMasterMapping findByUserLoginIdAndParentAndFeature(String userLoginId, String parent, String feature){
        return   dsl
                .selectFrom(CK_TEMP_MASTER_MAPPING)
                .where(CK_TEMP_MASTER_MAPPING.USERLOGINID.eq(userLoginId))
                .and(CK_TEMP_MASTER_MAPPING.FEATURE.eq(feature))
                .and(CK_TEMP_MASTER_MAPPING.PARENT.eq(parent))
                .fetchOneInto(TempMasterMapping.class);

    }

    public List<TempMasterMapping> findByFeatureAndActiveStatus(String feature, ActiveStatus activeStatus){
        return   dsl
                .selectFrom(CK_TEMP_MASTER_MAPPING)
                .where(CK_TEMP_MASTER_MAPPING.FEATURE.eq(feature))
                .and(CK_TEMP_MASTER_MAPPING.ACTIVE_STATUS.eq(activeStatus))
                .fetchInto(TempMasterMapping.class);
    }

    public List<TempMasterMapping> findByUserLoginIdAndFeatureAndActive(String userLoginId, String feature, ActiveStatus activeStatus){
        return   dsl
                .selectFrom(CK_TEMP_MASTER_MAPPING)
                .where(CK_TEMP_MASTER_MAPPING.USERLOGINID.eq(userLoginId))
                .and(CK_TEMP_MASTER_MAPPING.FEATURE.eq(feature))
                .and(CK_TEMP_MASTER_MAPPING.ACTIVE_STATUS.eq(activeStatus))
                .fetchInto(TempMasterMapping.class);
    }

    public TempMasterMapping refreshUsingJooq(TempMasterMapping cdmObject) {
        TempMasterMapping dbRecord = dsl
                .selectFrom(CK_TEMP_MASTER_MAPPING)
                .where(CK_TEMP_MASTER_MAPPING.USERLOGINID.eq(cdmObject.getUserLoginId()))
                .and(CK_TEMP_MASTER_MAPPING.PARENT.eq(cdmObject.getParent()))
                .and(CK_TEMP_MASTER_MAPPING.FEATURE.eq(cdmObject.getFeature()))
                .fetchOneInto(TempMasterMapping.class);

        if (dbRecord != null) {
            int version = dbRecord.getVersion();
            dbRecord.setActiveStatus(cdmObject.getActiveStatus());
            dbRecord.setExtendedAttributes(cdmObject.getExtendedAttributes());
            dbRecord.setVersion(version);
            dbRecord.setOldModel(cdmObject.getOldModel());

            return dbRecord;
        }else {
            cdmObject.setCreate(true);
            return cdmObject;
        }
    }

}
