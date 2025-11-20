package com.salescode.dim.repository;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeProductBifurcations;

import java.util.List;

public interface SchemeProductBifurcationRepo {
    List<SchemeProductBifurcations> findBySchemeId(String schemeId);
}
