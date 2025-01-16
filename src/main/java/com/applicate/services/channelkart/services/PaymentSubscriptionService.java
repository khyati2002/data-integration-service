package com.applicate.services.channelkart.services;


import com.applicate.services.channelkart.banking.models.PaymentSubscription;
import com.applicate.services.channelkart.repository.PaymentSubscriptionRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentSubscriptionService extends AbstractCDMService<PaymentSubscription> {

    private final PaymentSubscriptionRepository paymentRepository;

    public PaymentSubscriptionService(PaymentSubscriptionRepository paymentRepository) {
        super(paymentRepository);
        this.paymentRepository = paymentRepository;
    }

    public PaymentSubscription findByUserIdAndSupplierAndPaymentProviderType(String userId, String supplier, String paymentProviderType) {
        return paymentRepository.findByUserIdAndSupplierAndPaymentProvideType(userId, supplier, paymentProviderType);
    }


}
