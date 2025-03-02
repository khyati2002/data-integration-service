package com.salescode.channelkart.repository;

import com.salescode.jooq.generated.tables.pojos.CkSchemeOutletBifurcations;

import java.util.List;

public interface SchemeOutletBifurcationRepo {

    CkSchemeOutletBifurcations findBySchemeId(String schemeId);
}
