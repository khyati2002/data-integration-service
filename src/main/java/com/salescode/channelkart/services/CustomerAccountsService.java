package com.salescode.channelkart.services;


import com.salescode.channelkart.cache.AppCacheManager;
import com.salescode.channelkart.cache.DistributedCache;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.channelkart.repository.CustomerAccountsRepository;
import com.salescode.jooq.generated.tables.pojos.CkCustomerAccount;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerAccountsService extends AbstractCDMService<CkCustomerAccount> {
    public CustomerAccountsService() {
        super();
    }

    @Autowired
    private DistributedCache distributedCache;

    @Autowired
    private CustomerAccountsRepository customerAccountsRepository;

    public CkCustomerAccount getCustomerAccountInfo(String lob){
        return AppCacheManager.getInstance().withCache(null, "customerAccountInfo",
                (r) -> distributedCache.withCache(lob, null, "customerAccountInfo", (s) -> customerAccountsRepository.findByLob(lob)));
    }

public String getAdminLoginId(){
    String lob = SecurityContextUtils.getLob();
    if(lob.equals("none")){
        return "admin";
    }
    return getCustomerAccountInfo(lob).getName();
}


}
