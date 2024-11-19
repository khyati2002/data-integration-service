//package com.salescode.dataintegration.etl.cdm.repository;
//
//
//import com.applicate.services.channelkart.models.CustomerAccountInfo;
//import com.applicate.services.channelkart.models.enums.SubscriptionPlan;
//import com.salescode.jooq.generated.tables.pojos.CkCustomerAccount;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.stereotype.Repository;
//
//
//@Repository
//public interface CustomerAccountsRepository extends CommonJpaRepository<CustomerAccountInfo, String> {
//     public CkCustomerAccount findByLob(String lob);
//
//     @Query("select c.subscriptionPlan from CustomerAccountInfo c where c.lob = ?1")
//     SubscriptionPlan getSubscriptionPlan(String lob);
//
//     @Query("select c.timeZone from CustomerAccountInfo c where c.lob = ?1")
//     String getTimeZone(String lob);
//
//     CustomerAccountInfo findByName(String name);
//}
