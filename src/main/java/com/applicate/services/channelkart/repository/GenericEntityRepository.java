package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.GenericEntity;
import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_GENERIC_OBJECT;

public class GenericEntityRepository {
    private final DSLContext dsl;

    public GenericEntityRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<GenericEntity> findByNameAndKey1AndKey2(String name, String key1, String key2) {
        return dsl.selectFrom(CK_GENERIC_OBJECT)
                .where(CK_GENERIC_OBJECT.NAME.eq(name))
                .and(CK_GENERIC_OBJECT.KEY1.eq(key1))
                .and(CK_GENERIC_OBJECT.KEY2.eq(key2))
                .fetchInto(GenericEntity.class);
    }
}