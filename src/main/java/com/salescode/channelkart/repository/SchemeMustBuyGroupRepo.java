package com.salescode.channelkart.repository;

import com.salescode.jooq.generated.tables.pojos.CkSchemeMustBuyGroup;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemeMustBuyGroupRepo {
    CkSchemeMustBuyGroup findBySchemeId(String SchemeId);
}
