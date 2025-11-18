package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.GenericEntity;
import org.jooq.DSLContext;

import java.util.Collections;
import java.util.List;
import static com.salescode.dim.jooq.generated.Tables.CK_GENERIC_OBJECT;

public class GenericEntityRepository {
    private final DSLContext dsl;

    public GenericEntityRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<GenericEntity> findByName(String name) {
        return dsl.selectFrom(CK_GENERIC_OBJECT)
                .where(CK_GENERIC_OBJECT.NAME.eq(name))
                .fetchInto(GenericEntity.class);
    }

    public List<GenericEntity> findByNameAndKey1(String name, String key1) {
        return dsl.selectFrom(CK_GENERIC_OBJECT)
                .where(CK_GENERIC_OBJECT.NAME.eq(name))
                .and(CK_GENERIC_OBJECT.KEY1.eq(key1))
                .fetchInto(GenericEntity.class);
    }

    public List<GenericEntity> findByNameAndKey2(String name, String key2) {
        return dsl.selectFrom(CK_GENERIC_OBJECT)
                .where(CK_GENERIC_OBJECT.NAME.eq(name))
                .and(CK_GENERIC_OBJECT.KEY2.eq(key2))
                .fetchInto(GenericEntity.class);
    }

    public List<GenericEntity> findByNameAndKey1AndKey2(String name, String key1, String key2) {
        return dsl.selectFrom(CK_GENERIC_OBJECT)
                .where(CK_GENERIC_OBJECT.NAME.eq(name))
                .and(CK_GENERIC_OBJECT.KEY1.eq(key1))
                .and(CK_GENERIC_OBJECT.KEY2.eq(key2))
                .fetchInto(GenericEntity.class);
    }

    public List<GenericEntity> findByNameAndKey1OrderByLastModifiedTimeDesc(String name, String key1) {
        return dsl.selectFrom(CK_GENERIC_OBJECT)
                .where(CK_GENERIC_OBJECT.NAME.eq(name))
                .and(CK_GENERIC_OBJECT.KEY1.eq(key1))
                .orderBy(CK_GENERIC_OBJECT.LAST_MODIFIED_TIME.desc())
                .fetchInto(GenericEntity.class);
    }

    public List<GenericEntity> findByNameAndKey1InOrderByLastModifiedTimeDesc(String name, List<String> key1List) {
        if (key1List == null || key1List.isEmpty()) return Collections.emptyList();
        return dsl.selectFrom(CK_GENERIC_OBJECT)
                .where(CK_GENERIC_OBJECT.NAME.eq(name))
                .and(CK_GENERIC_OBJECT.KEY1.in(key1List))
                .orderBy(CK_GENERIC_OBJECT.LAST_MODIFIED_TIME.desc())
                .fetchInto(GenericEntity.class);
    }

    public List<GenericEntity> findByNameAndKey4(String name, String key4) {
        return dsl.selectFrom(CK_GENERIC_OBJECT)
                .where(CK_GENERIC_OBJECT.NAME.eq(name))
                .and(CK_GENERIC_OBJECT.KEY4.eq(key4))
                .fetchInto(GenericEntity.class);
    }

    public List<GenericEntity> findByNameAndKey2AndKey3OrderByCreationTimeDesc(String name, String key2, String key3) {
        return dsl.selectFrom(CK_GENERIC_OBJECT)
                .where(CK_GENERIC_OBJECT.NAME.eq(name))
                .and(CK_GENERIC_OBJECT.KEY2.eq(key2))
                .and(CK_GENERIC_OBJECT.KEY3.eq(key3))
                .orderBy(CK_GENERIC_OBJECT.CREATION_TIME.desc())
                .fetchInto(GenericEntity.class);
    }

}