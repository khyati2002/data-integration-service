package com.applicate.services.channelkart.services;

import org.jooq.DSLContext;

public interface DatabaseAwareService {

    void setDslContext(DSLContext dslContext);

}
