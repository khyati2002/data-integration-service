//package com.salescode.dataintegration.etl.cdm.repository;
//
//import com.salescode.jooq.generated.tables.pojos.CkAuthRole;
//import org.jooq.DSLContext;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//import static com.salescode.jooq.generated.tables.CkAuthRole.CK_AUTH_ROLE;
//@Repository
//public class RoleRepositoryImpl implements RoleRepository {
//
//    private final DSLContext dsl;
//
//
//    public RoleRepositoryImpl(DSLContext dsl) {
//        this.dsl = dsl;
//    }
//
//    @Override
//    public List<CkAuthRole> findByName(String name){
//        return dsl.selectFrom(CK_AUTH_ROLE)
//                .where(CK_AUTH_ROLE.NAME.eq(name))
//                .fetchInto(CkAuthRole.class);
//    }
//
//    public CkAuthRole findByNameIgnoreCaseContaining(String name){
//        return dsl.selectFrom(CK_AUTH_ROLE)
//                .where(CK_AUTH_ROLE.NAME.likeIgnoreCase("%" + name + "%"))
//                .fetchOneInto(CkAuthRole.class);
//    }
//
//    public CkAuthRole findByNameIgnoreCase(String name) {
//        return dsl.selectFrom(CK_AUTH_ROLE)
//                .where(CK_AUTH_ROLE.NAME.equalIgnoreCase(name))
//                .fetchOneInto(CkAuthRole.class);
//    }
//}
