package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.Tax;
import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.*;

public class TaxRepository {

    private final DSLContext dsl;

    public TaxRepository(DSLContext dsl) {
        this.dsl = dsl;
    }


}