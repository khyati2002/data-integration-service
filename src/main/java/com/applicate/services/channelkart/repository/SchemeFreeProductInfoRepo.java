package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.SchemeFreeproductinfo;

import java.util.List;

public interface SchemeFreeProductInfoRepo {
    List<SchemeFreeproductinfo> findBySchemeId(String schemeId);
}
