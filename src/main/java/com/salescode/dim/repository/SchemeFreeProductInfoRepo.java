package com.salescode.dim.repository;

import com.salescode.dim.jooq.generated.tables.pojos.SchemeFreeproductinfo;

import java.util.List;

public interface SchemeFreeProductInfoRepo {
    List<SchemeFreeproductinfo> findBySchemeId(String schemeId);
}
