package com.salescode.channelkart.validations.impl;



import com.salescode.channelkart.banking.models.Status;
import com.salescode.channelkart.models.HierarchyMetaData;
import com.salescode.channelkart.banking.models.PaymentSubscription;
import com.salescode.channelkart.models.User;
import com.salescode.channelkart.services.PaymentSubscriptionService;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.services.UserService;
import com.salescode.channelkart.validations.AbstractRule;
import com.salescode.channelkart.validations.RuleResult;
import com.salescode.channelkart.validations.ValidationResult;

import java.util.Optional;

public class FinaggUserMobileNoUpdateValidator extends AbstractRule<User> {

	private final UserService userService = SpringContext.getBean(UserService.class);
	private final PaymentSubscriptionService paymentSubscriptionService = SpringContext.getBean(PaymentSubscriptionService.class);
	
	@Override
	public RuleResult apply(User user) {
		if (user.getDesignation().contains("retailer")) {
			Optional<User> u = Optional.ofNullable(userService.findByLoginId(user.getLoginId()));
			if (u.isPresent() && u.get().getMobile() != null && !u.get().getMobile().isEmpty() && !u.get().getMobile().equalsIgnoreCase(user.getMobile())) {
				Optional<HierarchyMetaData> suppliers = user.getImmediateParent().stream().filter(sup -> getuserSubscription(user.getLoginId() ,sup.getImmediateParent())).findFirst();
				if (suppliers.isPresent()) {
					return new RuleResult(com.salescode.channelkart.validations.Status.ERROR, "Cannot update mobile no when onboarding status is pending or in-progress");
				}
			}
		}
		return RuleResult.OK;
	}

	private boolean getuserSubscription(String loginId, String supplier) {
		PaymentSubscription paymentSubscription = paymentSubscriptionService.findByUserIdAndSupplierAndPaymentProviderType(loginId, supplier, "FINAGG");
		return (paymentSubscription != null && (paymentSubscription.getStatus().equals(Status.PENDING) || paymentSubscription.getStatus().equals(Status.INPROGRESS)));

	}
	
}