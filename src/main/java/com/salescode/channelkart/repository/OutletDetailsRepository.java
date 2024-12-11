package com.salescode.channelkart.repository;

import com.salescode.jooq.dto.CkOutletDetailsDTO;
import com.salescode.jooq.impl.CkOutletDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface OutletDetailsRepository {

    CkOutletDetails findByOutletCode(String outletCode);
    CkOutletDetailsDTO populateDTOFromRepository(String outletcode);
}
