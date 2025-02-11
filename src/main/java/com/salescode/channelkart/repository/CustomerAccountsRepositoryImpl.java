package com.salescode.channelkart.repository;

import com.salescode.jooq.generated.tables.pojos.CkCustomerAccount;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static com.salescode.jooq.generated.tables.CkCustomerAccount.CK_CUSTOMER_ACCOUNT;
@Repository
public class CustomerAccountsRepositoryImpl implements CustomerAccountsRepository{
    private final DSLContext dsl;


    public CustomerAccountsRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }
    @Override
    public CkCustomerAccount findByLob(String lob) {
        return dsl.selectFrom(CK_CUSTOMER_ACCOUNT)
                .where(CK_CUSTOMER_ACCOUNT.LOB.eq(lob))
                .fetchOneInto(CkCustomerAccount.class);
    }

}
