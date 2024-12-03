package com.salescode.channelkart.services;


import com.salescode.jooq.generated.tables.pojos.CkCustomerAccount;

import org.springframework.stereotype.Service;

@Service
public class CustomerAccountsService extends AbstractCDMService<CkCustomerAccount> {
    public CustomerAccountsService() {
        super();
    }

}
