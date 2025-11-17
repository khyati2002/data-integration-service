package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.SchemeOutletBifurcations;

import java.util.List;

public interface SchemeOutletBifurcationRepo {
    List<SchemeOutletBifurcations> findBySchemeId(String schemeId);
}
