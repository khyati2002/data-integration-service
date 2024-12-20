package com.salescode.channelkart.services;


import com.salescode.channelkart.banking.models.PaymentSubscription;
import com.salescode.channelkart.repository.PaymentSubscriptionRepository;
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
