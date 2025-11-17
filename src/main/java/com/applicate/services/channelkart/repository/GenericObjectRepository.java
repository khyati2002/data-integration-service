package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.GenericObject;
import org.jooq.DSLContext;

import java.util.Optional;

import static com.salescode.dim.jooq.generated.Tables.CK_GENERIC_OBJECT;


public class GenericObjectRepository {

    private final DSLContext dsl;

    public GenericObjectRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<GenericObject> findByNameAndKey2AndKey3(String name, String key2){
        return dsl.selectFrom(CK_GENERIC_OBJECT)
                .where(CK_GENERIC_OBJECT.NAME.eq(name))
                .and(CK_GENERIC_OBJECT.KEY2.eq(key2))
                .fetchOptionalInto(GenericObject.class);
    }
}
