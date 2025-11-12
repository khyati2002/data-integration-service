package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.GenericEntity;
import org.jooq.DSLContext;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.salescode.dim.jooq.generated.Tables.CK_GENERIC_OBJECT;
import static com.salescode.dim.jooq.generated.Tables.CK_HIERARCHY_METADATA;

public class GenericEntityRepository {
    private final DSLContext dsl;

    public GenericEntityRepository(DSLContext dsl){
        this.dsl = dsl;
    }

    public List<GenericEntity> readModelsByName(String name) {
        return dsl.selectFrom(CK_GENERIC_OBJECT)
                .where(CK_GENERIC_OBJECT.NAME.eq(name))
                .fetchInto(GenericEntity.class);

    }



    public List<GenericEntity> findModelsByNameandKey1(String name, String key1){

        return dsl.selectFrom(CK_GENERIC_OBJECT)
                        .where(CK_GENERIC_OBJECT.NAME.eq(name))
                        .and(CK_GENERIC_OBJECT.KEY1.eq(key1))
                        .fetchInto(GenericEntity.class);
    }

}
