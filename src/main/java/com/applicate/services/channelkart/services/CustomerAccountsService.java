package com.applicate.services.channelkart.services;

import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.jooq.generated.tables.pojos.CustomerAccount;
import com.salescode.dim.jooq.impl.User;

import static com.salescode.dim.jooq.generated.Tables.CK_CUSTOMER_ACCOUNT;
import static com.salescode.dim.jooq.generated.Tables.CK_USER;

public class CustomerAccountsService extends AbstractCDMService<CustomerAccount> {

    public String getAdminLoginId() {
        return getAdminInfo().getLoginid();
    }

    @Cacheable
    public User getAdminInfo() {
        com.salescode.dim.jooq.generated.tables.pojos.User user = getDslContext().select()
                .from(CK_CUSTOMER_ACCOUNT)
                .join(CK_USER)
                .on(CK_CUSTOMER_ACCOUNT.USERNAME.eq(CK_USER.LOGINID))
                .fetchOneInto(com.salescode.dim.jooq.generated.tables.pojos.User.class);
        return User.of(user);
    }

    public String getAdminHierarchy(String inUser) {
        return inUser + " > " + getAdminLoginId();
    }

}
