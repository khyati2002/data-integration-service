package com.salescode.dim.repository;

import com.salescode.dim.jooq.generated.tables.pojos.SchemeLocationBifurcations;

import java.util.List;

public interface SchemeLocationBifurcationRepo {
    List<SchemeLocationBifurcations> findBySchemeId(String schemeId);
}
