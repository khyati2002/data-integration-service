package com.applicate.services.channelkart.services;

import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.jooq.impl.User;

import static com.salescode.dim.jooq.generated.Tables.CK_USER;

public class UserService extends AbstractCDMService<User> {
    public static final String DEFAULT_ENCODED_PASSWORD = "$2a$10$GetnNjgilfLkIv.2R3nHMevLZfI9HGHWQ3iXw3nrCfJlrpePirkIi";

    @Cacheable
    public User findByLoginId(String loginid) {
        com.salescode.dim.jooq.generated.tables.pojos.User user = getDslContext().selectFrom(CK_USER)
                .where(CK_USER.LOGINID.eq(loginid))
                .fetchOneInto(com.salescode.dim.jooq.generated.tables.pojos.User.class);
        return User.of(user);
    }
}
