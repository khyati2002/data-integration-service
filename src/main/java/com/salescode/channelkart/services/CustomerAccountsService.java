package com.salescode.channelkart.services;



import com.salescode.channelkart.cache.AppCacheManager;
import com.salescode.channelkart.cache.DistributedCache;
import com.salescode.channelkart.models.CustomerAccountInfo;
import com.salescode.channelkart.repository.CustomerAccountsRepository;
import com.salescode.channelkart.security.SecurityContextUtils;
import org.springframework.stereotype.Service;

@Service
public class CustomerAccountsService extends AbstractCDMService<CustomerAccountInfo> {

    private final DistributedCache distributedCache;
    private CustomerAccountsRepository customerAccountsRepository;

    public CustomerAccountsService(CustomerAccountsRepository repository, DistributedCache distributedCache) {
        super(repository);
        this.customerAccountsRepository = repository;
        this.distributedCache = distributedCache;
    }

    public String getTimeZone() {

//        String timeZone = SecurityContextUtils.getTimeZone();
//
//        if(timeZone!=null)
//            return timeZone;

        return getTimeZone(SecurityContextUtils.getLob());
    }

    public String getTimeZone(String lob) {
        CustomerAccountInfo customerAccountInfo = getCustomerAccountInfo(lob);
        if(customerAccountInfo == null) {
            throw new IllegalArgumentException("Customer account not found for lob '"+lob+"'. Please check if customer account defined with non-empty column lob.");
        }
        return customerAccountInfo.getTimeZone();
    }

    public CustomerAccountInfo getCustomerAccountInfo(String lob){
        return distributedCache.withCache(lob,null,"customerAccountInfo",(s)->customerAccountsRepository.findByLob(lob));
    }

    public String getAdminLoginId(){
        String lob = SecurityContextUtils.getLob();
        if(lob.equals("none")){
            return "admin";
        }
        return getCustomerAccountInfo(lob).getAdmin().getLoginId();
    }
    public String getAdminHierarchy(String inUser){
        return inUser+ " > " +getAdminLoginId();
    }
}
