package com.salescode.dim.repository;

import com.salescode.dim.jooq.generated.tables.pojos.SchemeMustBuyGroup;

import java.util.List;

public interface SchemeMustBuyGroupRepo {
    List<SchemeMustBuyGroup> findBySchemeId(String schemeId);
}
