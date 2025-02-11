package com.salescode.channelkart.repository;

import com.salescode.jooq.generated.tables.pojos.CkUserParent;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import static com.salescode.jooq.generated.tables.CkUserParent.CK_USER_PARENT;
@Repository
public class UserParentRepositoryImpl implements UserParentRepository{
    private final DSLContext dsl;


    public UserParentRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }
    @Override
    public List<CkUserParent> findByUserLoginId(String loginId) {
       return dsl.selectFrom(CK_USER_PARENT)
               .where(CK_USER_PARENT.USERLOGINID.eq(loginId))
               .fetchInto(CkUserParent.class);
    }

    @Override
    public CkUserParent findByUserLoginIdAndParent(String loginid, String parentloginid) {
        return dsl.selectFrom(CK_USER_PARENT)
                .where(CK_USER_PARENT.USERLOGINID.eq(loginid))
                .and(CK_USER_PARENT.PARENT.eq(parentloginid))
                .fetchOneInto(CkUserParent.class);
    }

    @Override
    public void deleteByUserLoginId(String loginid) {
       dsl.deleteFrom(CK_USER_PARENT)
               .where(CK_USER_PARENT.USERLOGINID.eq(loginid))
               .execute();
    }

    @Override
    public void deleteByUserLoginIdIn(Collection<String> loginid) {
        dsl.deleteFrom(CK_USER_PARENT)
                .where(CK_USER_PARENT.USERLOGINID.in(loginid))
                .execute();
    }

    @Override
    public List<CkUserParent> findByParentIn(List<String> parents) {
        return dsl.selectFrom(CK_USER_PARENT)
                .where(CK_USER_PARENT.PARENT.in(parents))
                .fetchInto(CkUserParent.class);
    }

    @Override
    public List<Map<String, Object>> getUserParentMapping(List<String> outletList) {
        return dsl.select(CK_USER_PARENT.USERLOGINID, CK_USER_PARENT.PARENT)
                .from(CK_USER_PARENT)
                .where(CK_USER_PARENT.USERLOGINID.in(outletList))
                .fetch()
                .intoMaps(); // Converts result to List of Map<String, Object>
    }
}
