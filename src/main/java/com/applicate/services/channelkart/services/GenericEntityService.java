//package com.applicate.services.channelkart.services;
//
//import com.applicate.services.channelkart.services.AbstractCDMService;
//import com.salescode.dim.jooq.generated.tables.pojos.CustomerAccount;
//import com.salescode.dim.jooq.generated.tables.pojos.User;
//import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
//import org.jooq.DSLContext;
//import org.jooq.Record3;
//import org.jooq.Result;
//
//import static com.salescode.dim.jooq.generated.Tables.*;
//import static com.salescode.dim.jooq.generated.Tables.CK_GENERIC_OBJECT;
//
//public class GenericEntityService extends AbstractCDMService<CustomerAccount> {
//
//    private final DSLContext dsl;
//    public GenericEntityService(DSLContext dsl) {
//        super(dsl);
//        this.dsl = dsl;
//    }
//
//    public  Result<Record3<String, String, JsonNode>> getChannel(){
//        Result<Record3<String, String, JsonNode>> channelName = dsl.select(
//                        CK_GENERIC_OBJECT.KEY1,
//                        CK_GENERIC_OBJECT.KEY2,
//                        CK_GENERIC_OBJECT.PAYLOAD)
//                .from(CK_GENERIC_OBJECT)
//                .where(CK_GENERIC_OBJECT.NAME.eq("entity-group"))
//                .fetch();
//        return channelName;
//    }
//
//    public User getAdminInfo(){
//        User admin = dsl.select()
//                .from(CK_CUSTOMER_ACCOUNT)
//                .join(CK_USER)
//                .on(CK_CUSTOMER_ACCOUNT.USERNAME.eq(CK_USER.LOGINID))
//                .fetchOneInto(User.class);
//        return admin;
//    }
//
//    public String getAdminHierarchy(String inUser){
//        return inUser+ " > " +getAdminLoginId();
//    }
//
//    @Override
//    public CustomerAccount save(CustomerAccount cdmObject) {
//        return null;
//    }
//}
