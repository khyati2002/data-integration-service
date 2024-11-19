//package com.salescode.dataintegration.etl.cdm.repository;
//
//import com.salescode.jooq.generated.tables.pojos.CkCustomerAccount;
//import org.jooq.DSLContext;
//import static com.salescode.jooq.generated.tables.CkCustomerAccount.CK_CUSTOMER_ACCOUNT;
//public class CustomerAccountsRepositoryImpl implements CustomerAccountsRepository{
//    private final DSLContext dsl;
//
//
//    public CustomerAccountsRepositoryImpl(DSLContext dsl) {
//        this.dsl = dsl;
//    }
//    @Override
//    public CkCustomerAccount findByLob(String lob) {
//        return dsl.selectFrom(CK_CUSTOMER_ACCOUNT)
//                .where(CK_CUSTOMER_ACCOUNT.LOB.eq(lob))
//                .fetchOneInto(CkCustomerAccount.class);
//    }
//
//    @Override
//    public SubscriptionPlan getSubscriptionPlan(String lob) {
//        return null;
//    }
//
//    @Override
//    public String getTimeZone(String lob) {
//        return "";
//    }
//
//    @Override
//    public CustomerAccountInfo findByName(String name) {
//        return null;
//    }
//}
