package com.applicate.bepensa.validation;
import com.applicate.services.channelkart.models.DeliveryPJP;
import com.applicate.services.channelkart.models.OutletDetails;
import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.validations.AbstractRule;
import com.applicate.services.channelkart.validations.RuleResult;
import com.applicate.services.channelkart.validations.Status;

import java.util.Set;

public class Deliverypjpvalidator extends AbstractRule<DeliveryPJP> {
    private final UserService userService = SpringContext.getBean(UserService.class);
    private final OutletDetailsService outletService = SpringContext.getBean(OutletDetailsService.class);

    @Override
    public RuleResult apply(DeliveryPJP deliveryPJP) {
        boolean isSupplierPresent = isSupplierPresent(deliveryPJP.getLoginId());
        boolean isOutletPresent = isOutletPresent(deliveryPJP.getOutletCode());

        if (!isSupplierPresent && !isOutletPresent) {
            return new RuleResult(Status.ERROR, "Sales rep and outlet are not present in our system");
        } else if (!isSupplierPresent) {
            return new RuleResult(Status.ERROR, "Sales rep is not present in our system");
        } else if (!isOutletPresent) {
            return new RuleResult(Status.ERROR, "Outlet is not present in our system");
        }

        return RuleResult.OK;
    }

    private boolean isSupplierPresent(String salesrep) {
        User user = userService.findByLoginId(salesrep);
        if (user != null) {
            Set<String> designations = user.getDesignation();
            for (String designation : designations) {
                if (designation.equalsIgnoreCase("salesrep")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isOutletPresent(String outlet) {
        OutletDetails dOutlet = outletService.findByOutletCode(outlet);
        return dOutlet != null;
    }
}
