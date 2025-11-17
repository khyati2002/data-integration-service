package com.applicate.services.channelkart.validations.repository;


import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.User;

import java.util.Optional;

public class FinaggUserMobileNoUpdateValidator extends AbstractValidationRule<User>{

	private final UserService userService = (UserService) ServiceLocator.lookup(User.class);
//	private final PaymentSubscriptionService paymentSubscriptionService = SpringContext.getBean(PaymentSubscriptionService.class);
	
	@Override
	public OperationResult.StepResult apply(User user) {
		if (user.getDesignation().contains("retailer")) {
			Optional<com.salescode.dim.jooq.generated.tables.pojos.User> u = Optional.ofNullable(userService.findByLoginId(user.getLoginid(), true));
			if (u.isPresent() && u.get().getMobile() != null && !u.get().getMobile().isEmpty() && !u.get().getMobile().equalsIgnoreCase(user.getMobile())) {
				Optional<HierarchyMetadata> suppliers = user.getImmediateParent().stream().filter(sup -> getuserSubscription(user.getLoginid() ,sup.getParent())).findFirst();
				if (suppliers.isPresent()) {
					return new OperationResult.StepResult(OperationResult.Status.ERROR, "Cannot update mobile no when onboarding status is pending or in-progress");
				}
			}
		}
		return OperationResult.StepResult.OK;
	}

	private boolean getuserSubscription(String loginId, String supplier) {
//		PaymentSubscription paymentSubscription = paymentSubscriptionService.findByUserIdAndSupplierAndPaymentProviderType(loginId, supplier, "FINAGG");
//		return (paymentSubscription != null && (paymentSubscription.getStatus().equals(Status.PENDING) || paymentSubscription.getStatus().equals(Status.INPROGRESS)));
   return false;
	}
	
}