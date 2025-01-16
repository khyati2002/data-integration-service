package com.applicate.services.channelkart.services;



import com.applicate.services.channelkart.cache.AppCacheManager;
import com.applicate.services.channelkart.cache.DistributedCache;
import com.applicate.services.channelkart.models.CustomerAccountInfo;
import com.applicate.services.channelkart.repository.CustomerAccountsRepository;
import com.applicate.services.channelkart.security.SecurityContextUtils;
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
        return AppCacheManager.getInstance().withCache(null, "customerAccountInfo",
                (r) -> distributedCache.withCache(lob, null, "customerAccountInfo", (s) -> customerAccountsRepository.findByLob(lob)));
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
