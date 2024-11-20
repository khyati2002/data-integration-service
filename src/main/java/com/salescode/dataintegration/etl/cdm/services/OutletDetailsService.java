package com.salescode.dataintegration.etl.cdm.services;

import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.jooq.generated.tables.pojos.CkOutletDetails;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.TableImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Slf4j
@Service
public class OutletDetailsService extends AbstractCDMService<CkOutletDetails> {

    private final DSLContext dsl;

    public OutletDetailsService(DSLContext dsl) {
        log.info("OutletDetailsService created");
        this.dsl = dsl;
    }

}
