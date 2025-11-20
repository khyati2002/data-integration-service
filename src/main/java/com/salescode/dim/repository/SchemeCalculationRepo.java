package com.salescode.dim.repository;

import com.salescode.dim.jooq.impl.SchemeCalculation;

public interface SchemeCalculationRepo {
    SchemeCalculation findBySchemeId(String schemeId);
}
