package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.AuthRole;
import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_AUTH_ROLE;
import static org.jooq.impl.DSL.lower;

public class RoleRepository {

    private final DSLContext dsl;

    public RoleRepository(DSLContext dsl){
        this.dsl = dsl;
    }

   public AuthRole findByNameIgnoreCase(String name){
       AuthRole role = dsl
               .selectFrom(CK_AUTH_ROLE)
               .where(lower(CK_AUTH_ROLE.NAME).eq(name.toLowerCase())) // Case-insensitive comparison
               .fetchOneInto(AuthRole.class);
       return role;
   }

   public List<AuthRole> findByNameIn(List<String> roleNames) {
       return dsl.selectFrom(CK_AUTH_ROLE)
               .where(CK_AUTH_ROLE.NAME.in(roleNames))
               .fetchInto(AuthRole.class);
   }
}
