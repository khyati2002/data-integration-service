package com.salescode.channelkart.repository.impl;

import com.salescode.channelkart.repository.DivisionRepository;
import com.salescode.jooq.generated.tables.pojos.CkDivision;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

import static com.salescode.jooq.generated.tables.CkDivision.CK_DIVISION;

@Repository
public class DivisionRepositoryImpl implements DivisionRepository {
    private final DSLContext dsl;

    public DivisionRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public CkDivision findByDivisionName(String divisionName) {
        return null;
    }

    @Override
    public Collection<CkDivision> findByOrderByLevelAsc() {
        return List.of();
    }

    @Override
    public Collection<CkDivision> findByChannelDivisionOrderByLevelAsc(boolean isChannelDivision) {
        return dsl.selectFrom(CK_DIVISION)
                .where(CK_DIVISION.CHANNEL_DIVISION.eq(isChannelDivision))  // Filter by channel_division flag
                .orderBy(CK_DIVISION.LEVEL.asc())  // Order by the level in ascending order
                .fetchInto(CkDivision.class);
    }

    @Override
    public List<CkDivision> findByParent(String divisionName) {
        return List.of();
    }
}
