package com.salescode.channelkart.repository;

import com.salescode.jooq.generated.tables.pojos.CkSchemeLocationBifurcations;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemeLocationBifurcationRepo {
    CkSchemeLocationBifurcations findBySchemeId(String schemeId);
}
