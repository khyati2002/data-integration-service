package com.salescode.dim.repository;

import com.salescode.dim.jooq.generated.tables.pojos.SchemeCalculation;

public interface SchemeCalculationRepo {
    SchemeCalculation findBySchemeId(String schemeId);
}
