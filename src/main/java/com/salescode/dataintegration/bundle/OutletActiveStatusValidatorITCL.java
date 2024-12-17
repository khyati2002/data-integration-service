package com.salescode.dataintegration.bundle;


import com.salescode.channelkart.models.OutletDetails;
import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.services.OutletDetailsService;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.dataintegration.etl.validation.AbstractValidationRule;
import com.salescode.dataintegration.etl.validation.RuleResult;
import com.salescode.dataintegration.etl.validation.ValidationResult;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Checks if the Outlet is being activated.
 * If activeStatus is being changed to Active, throw error message.
 */
public class OutletActiveStatusValidatorITCL  extends AbstractValidationRule<OutletDetails> {

    private static final OutletDetailsService outletDetailsService = SpringContext.getBean(OutletDetailsService.class);

    public RuleResult apply(OutletDetails cdm) {
        OutletDetails dbRecord = outletDetailsService.findByOutletCode(cdm.getOutletCode());

        if(cdm.getOutletCode()=="auto_generated"){
            return new RuleResult(ValidationResult.Status.ERROR,"null values are not allowed in outletCode column");
        }
        if(ObjectUtils.isEmpty(dbRecord)){
            return new RuleResult(ValidationResult.Status.ERROR,"UID is not present in our system");
        }

        if(dbRecord.getActiveStatus().equals(ActiveStatus.INACTIVE) && cdm.getActiveStatus().equals(ActiveStatus.ACTIVE)){
            return new RuleResult(ValidationResult.Status.ERROR,"User activation access is blocked for admin users.");
        }

        return RuleResult.OK;
    }

}
