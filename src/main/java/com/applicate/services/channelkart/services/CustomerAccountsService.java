package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.cache.AppCacheManager;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.cache.CacheKeys;
import com.salescode.dim.jooq.generated.tables.pojos.CustomerAccount;
import com.salescode.dim.jooq.impl.User;

import static com.salescode.dim.jooq.generated.Tables.CK_CUSTOMER_ACCOUNT;
import static com.salescode.dim.jooq.generated.Tables.CK_USER;

public class CustomerAccountsService extends AbstractCDMService<CustomerAccount> {

    public String getAdminLoginId() {
        return getAdminInfo().getLoginid();
    }

    public User getAdminInfo() {

        return AppCacheManager.getInstance().withCache(CacheKeys.CUSTOMER_ACCOUNT_INFO_CACHE_DOMAIN,"admin-info", k -> {
            com.salescode.dim.jooq.generated.tables.pojos.User user =
                    getDslContext().select()
                            .from(CK_CUSTOMER_ACCOUNT)
                            .join(CK_USER)
                            .on(CK_CUSTOMER_ACCOUNT.USERNAME.eq(CK_USER.LOGINID))
                            .fetchOneInto(com.salescode.dim.jooq.generated.tables.pojos.User.class);
            if (user == null) return null;
            return User.of(user);
        });
    }

    public String getAdminHierarchy(String inUser) {
        return inUser + " > " + getAdminLoginId();
    }

    public String getTimeZone() {
        AppCacheManager cacheManager = AppCacheManager.getInstance();
        return cacheManager.withCache(CacheKeys.CUSTOMER_ACCOUNT_INFO_CACHE_DOMAIN, "time-zone", k -> getDslContext().select(CK_CUSTOMER_ACCOUNT.TIME_ZONE).from(CK_CUSTOMER_ACCOUNT).fetchOneInto(String.class));
    }

    public CustomerAccount getCustomerAccountInfo() {
        String lob = SecurityContextUtils.getLob();
        AppCacheManager cacheManager = AppCacheManager.getInstance();
        return cacheManager.withCache(CacheKeys.CUSTOMER_ACCOUNT_INFO_CACHE_DOMAIN, lob, k -> getDslContext().selectFrom(CK_CUSTOMER_ACCOUNT).fetchOneInto(CustomerAccount.class));
    }
}
