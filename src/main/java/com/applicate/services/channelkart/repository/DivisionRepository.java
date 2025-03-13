package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.Division;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_DIVISION;

public class DivisionRepository {
    private final DSLContext dsl;

    public DivisionRepository(DSLContext dsl){
        this.dsl = dsl;
    }
    public Collection<Division> findByChannelDivisionOrderByLevelAsc(boolean isChannelDivision){
        List<Division> divisions = dsl
                .selectFrom(CK_DIVISION)
                .where(CK_DIVISION.CHANNEL_DIVISION.eq(isChannelDivision))
                .orderBy(CK_DIVISION.LEVEL.asc())
                .fetchInto(Division.class);
        return divisions;
    }
}
