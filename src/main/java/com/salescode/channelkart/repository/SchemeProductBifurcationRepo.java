package com.salescode.channelkart.repository;

import com.salescode.jooq.generated.tables.pojos.CkSchemeProductBifurcations;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchemeProductBifurcationRepo {
    CkSchemeProductBifurcations findBySchemeId(String schemeId);
}
