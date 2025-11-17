package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.SchemeDefination;

public interface SchemeDefinationRepo {


    SchemeDefination findBySchemeId(String schemeId);
}
