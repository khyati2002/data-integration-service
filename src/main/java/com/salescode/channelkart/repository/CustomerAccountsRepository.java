package com.salescode.channelkart.repository;


import com.salescode.jooq.generated.tables.pojos.CkCustomerAccount;
import org.springframework.stereotype.Repository;


@Repository
public interface CustomerAccountsRepository {
     public CkCustomerAccount findByLob(String lob);

}
