package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.jooq.generated.tables.pojos.User;
import org.jooq.DSLContext;

import static com.salescode.dim.jooq.generated.Tables.CK_CUSTOMER_ACCOUNT;
import static com.salescode.dim.jooq.generated.Tables.CK_USER;

public class CustomerAccountsService extends AbstractCDMService {

    private final DSLContext dsl;
    public CustomerAccountsService(DSLContext dsl) {
        super(dsl);
        this.dsl = dsl;
    }

    public String getAdminLoginId(){
        User admin = dsl.select()
                .from(CK_CUSTOMER_ACCOUNT)
                .join(CK_USER)
                .on(CK_CUSTOMER_ACCOUNT.USERNAME.eq(CK_USER.LOGINID))
                .fetchOneInto(User.class);
        return admin.getLoginid();
    }

    @Override
    public CommonDataModel save(CommonDataModel cdmObject) {
        return null;
    }
}
