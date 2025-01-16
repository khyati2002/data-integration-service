package com.salescode.dataintegration.bundle;


import com.applicate.services.channelkart.models.OutletDetails;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.validations.AbstractRule;
import com.applicate.services.channelkart.validations.RuleResult;
import com.applicate.services.channelkart.validations.Status;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Checks if the Outlet is being activated.
 * If activeStatus is being changed to Active, throw error message.
 */
public class OutletActiveStatusValidatorITCL  extends AbstractRule<OutletDetails> {

    private static final OutletDetailsService outletDetailsService = SpringContext.getBean(OutletDetailsService.class);

    public RuleResult apply(OutletDetails cdm) {
        OutletDetails dbRecord = outletDetailsService.findByOutletCode(cdm.getOutletCode());

        if(cdm.getOutletCode()=="auto_generated"){
            return new RuleResult(Status.ERROR,"null values are not allowed in outletCode column");
        }
        if(ObjectUtils.isEmpty(dbRecord)){
            return new RuleResult(Status.ERROR,"UID is not present in our system");
        }

        if(dbRecord.getActiveStatus().equals(ActiveStatus.INACTIVE) && cdm.getActiveStatus().equals(ActiveStatus.ACTIVE)){
            return new RuleResult(Status.ERROR,"User activation access is blocked for admin users.");
        }

        return RuleResult.OK;
    }

}
