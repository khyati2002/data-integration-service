package com.salescode.channelkart.repository;

import com.salescode.jooq.generated.tables.pojos.CkSchemeCalculation;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemeCalculationRepo {
    CkSchemeCalculation findBySchemeId(String schemeId);
}
