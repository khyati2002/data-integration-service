package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.EntityParentMapping;
import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_ENTITY_PARENT_MAPPING;

public class EntityParentMappingRepository {
    private final DSLContext dsl;

    public EntityParentMappingRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<EntityParentMapping> findAll() {
        return dsl.selectFrom(CK_ENTITY_PARENT_MAPPING)
                .fetchInto(EntityParentMapping.class);
    }

    public EntityParentMapping findById(String id) {
        return dsl.selectFrom(CK_ENTITY_PARENT_MAPPING)
                .where(CK_ENTITY_PARENT_MAPPING.ID.eq(id))
                .fetchOneInto(EntityParentMapping.class);
    }
}