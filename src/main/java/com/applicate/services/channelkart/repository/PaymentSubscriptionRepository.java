package com.applicate.services.channelkart.repository;


import com.applicate.services.channelkart.banking.models.PaymentSubscription;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentSubscriptionRepository extends CommonJpaRepository<PaymentSubscription, String >{


	PaymentSubscription findByUserIdAndSupplierAndPaymentProvideType(String userId, String supplier,String paymentProviderType);
}
