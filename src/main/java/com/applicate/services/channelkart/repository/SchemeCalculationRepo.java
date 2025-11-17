package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.SchemeCalculation;

public interface SchemeCalculationRepo {
    SchemeCalculation findBySchemeId(String schemeId);
}
