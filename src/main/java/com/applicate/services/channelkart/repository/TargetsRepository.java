package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import com.salescode.dim.jooq.generated.tables.pojos.TargetResults;
import com.salescode.dim.jooq.impl.Targets;
import org.jooq.DSLContext;

import java.util.Optional;

import static com.salescode.dim.jooq.generated.Tables.CK_METADATA;
import static com.salescode.dim.jooq.generated.Tables.CK_TARGETS;


public class TargetsRepository {

    private final DSLContext dsl;

    public TargetsRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<TargetResults> findByTargetId(String targetId){
        return dsl.selectFrom(CK_TARGETS)
                .where(CK_TARGETS.TARGET_ID.eq(targetId))
                .fetchOptionalInto(TargetResults.class);
    }

}
