package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.TempMasterMapping;
import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_TEMP_MASTER_MAPPING;

public class TempMasterMappingRepository {

    private final DSLContext dsl;

    public TempMasterMappingRepository(DSLContext dsl){
        this.dsl = dsl;
    }

    public List<TempMasterMapping> findByUserLoginIdAndFeature(String userLoginId, String feature) {
        return dsl.selectFrom(CK_TEMP_MASTER_MAPPING)
                .where(CK_TEMP_MASTER_MAPPING.USERLOGINID.eq(userLoginId))
                .and(CK_TEMP_MASTER_MAPPING.FEATURE.eq(feature))
                .fetchInto(TempMasterMapping.class);
    }
}
