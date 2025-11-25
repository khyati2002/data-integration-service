package com.salescode.dim.repository;

import com.salescode.dim.jooq.impl.SchemeDefination;

public interface SchemeDefinationRepo {


    SchemeDefination findBySchemeId(String schemeId);
}
