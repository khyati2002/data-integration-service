package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.UserParent;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.salescode.dim.jooq.generated.Tables.CK_USER_PARENT;


public class UserParentRepository{
    private final DSLContext dsl;


    public UserParentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }
     
    public List<UserParent> findByUserLoginId(String loginId) {
       return dsl.selectFrom(CK_USER_PARENT)
               .where(CK_USER_PARENT.USERLOGINID.eq(loginId))
               .fetchInto(UserParent.class);
    }

     
    public UserParent findByUserLoginIdAndParent(String loginid, String parentloginid) {
        return dsl.selectFrom(CK_USER_PARENT)
                .where(CK_USER_PARENT.USERLOGINID.eq(loginid))
                .and(CK_USER_PARENT.PARENT.eq(parentloginid))
                .fetchOneInto(UserParent.class);
    }

     
    public void deleteByUserLoginId(String loginid) {
       dsl.deleteFrom(CK_USER_PARENT)
               .where(CK_USER_PARENT.USERLOGINID.eq(loginid))
               .execute();
    }

     
    public void deleteByUserLoginIdIn(Collection<String> loginid) {
        dsl.deleteFrom(CK_USER_PARENT)
                .where(CK_USER_PARENT.USERLOGINID.in(loginid))
                .execute();
    }

     
    public List<UserParent> findByParentIn(List<String> parents) {
        return dsl.selectFrom(CK_USER_PARENT)
                .where(CK_USER_PARENT.PARENT.in(parents))
                .fetchInto(UserParent.class);
    }

     
    public List<Map<String, Object>> getUserParentMapping(List<String> outletList) {
        return dsl.select(CK_USER_PARENT.USERLOGINID, CK_USER_PARENT.PARENT)
                .from(CK_USER_PARENT)
                .where(CK_USER_PARENT.USERLOGINID.in(outletList))
                .fetch()
                .intoMaps(); // Converts result to List of Map<String, Object>
    }
}
