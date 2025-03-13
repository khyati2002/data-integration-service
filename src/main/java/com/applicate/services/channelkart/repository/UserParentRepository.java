package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.UserParent;
import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_USER_PARENT;

public class UserParentRepository {
    private final DSLContext dsl;


    public UserParentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<UserParent> findByUserLoginId(String loginId) {
        return dsl.selectFrom(CK_USER_PARENT)
                .where(CK_USER_PARENT.USERLOGINID.eq(loginId))
                .fetchInto(UserParent.class);
    }
}
