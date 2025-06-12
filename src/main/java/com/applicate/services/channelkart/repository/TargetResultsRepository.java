package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.TargetResults;
import com.salescode.dim.jooq.impl.Targets;
import org.jooq.DSLContext;

import java.util.Optional;

import static com.salescode.dim.jooq.generated.Tables.CK_TARGETS;
import static com.salescode.dim.jooq.generated.Tables.CK_TARGET_RESULTS;


public class TargetResultsRepository {

    private final DSLContext dsl;

    public TargetResultsRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<TargetResults> findByTargetId(String targetId){
        return dsl.selectFrom(CK_TARGET_RESULTS)
                .where(CK_TARGETS.TARGET_ID.eq(targetId))
                .fetchOptionalInto(TargetResults.class);
    }

}
