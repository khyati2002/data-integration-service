package com.salescode.channelkart.repository.impl;

import com.salescode.channelkart.repository.OutletDetailsRepository;
import com.salescode.jooq.dto.CkOutletDetailsDTO;
import com.salescode.jooq.generated.tables.pojos.CkHierarchyMetadata;
import com.salescode.jooq.impl.CkOutletDetails;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.salescode.jooq.generated.Tables.CK_HIERARCHY_METADATA;
import static com.salescode.jooq.generated.Tables.CK_OUTLET_DETAILS;

@Repository
public class OutletDetailsRepositoryImpl implements OutletDetailsRepository {
    private final DSLContext dsl;

    public OutletDetailsRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public CkOutletDetails findByOutletCode(String outletCode) {
        return dsl.selectFrom(CK_OUTLET_DETAILS)
                .where(CK_OUTLET_DETAILS.OUTLETCODE.eq(outletCode))
                .fetchOneInto(CkOutletDetails.class);
    }

    public List<CkHierarchyMetadata> getImmediateParent(String outletcode){
        return dsl.selectFrom(CK_HIERARCHY_METADATA)
                .where(CK_HIERARCHY_METADATA.PARENT.eq(outletcode))
                .fetchInto(CkHierarchyMetadata.class);
    }

    @Override
    public CkOutletDetailsDTO populateDTOFromRepository(String outletcode){
        CkOutletDetailsDTO ckOutletInfo = new CkOutletDetailsDTO();
        ckOutletInfo.setImmediateParent(getImmediateParent(outletcode));
        return ckOutletInfo;
    }
}
