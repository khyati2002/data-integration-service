package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.AuthRole;
import com.salescode.dim.jooq.generated.tables.pojos.Division;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.*;

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

    public Collection<Division> findByOrderByLevelAsc() {
        return dsl.selectFrom(CK_DIVISION)
                .orderBy(CK_DIVISION.LEVEL.asc())
                .fetchInto(Division.class);
    }

    public List<AuthRole> findRolesByDivisionId(String divisionId) {
        return dsl.select(CK_AUTH_ROLE.asterisk())
                .from(CK_AUTH_ROLE)
                .join(CK_DIVISION_ROLES).on(CK_DIVISION_ROLES.ROLES_ID.eq(CK_AUTH_ROLE.ID))
                .where(CK_DIVISION_ROLES.DIVISION_ID.eq(divisionId))
                .fetchInto(AuthRole.class);
    }


}
