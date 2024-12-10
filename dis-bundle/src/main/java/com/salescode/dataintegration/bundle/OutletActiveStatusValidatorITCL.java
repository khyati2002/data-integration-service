package com.salescode.channelkart.validations.impl;


import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.services.OutletDetailsService;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.dataintegration.etl.validation.AbstractValidationRule;
import com.salescode.dataintegration.etl.validation.RuleResult;
import com.salescode.dataintegration.etl.validation.ValidationResult;
import com.salescode.jooq.generated.tables.pojos.CkOutletDetails;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Checks if the Outlet is being activated.
 * If activeStatus is being changed to Active, throw error message.
 */
public class OutletActiveStatusValidatorITCL extends AbstractValidationRule<CkOutletDetails> {

    private static final OutletDetailsService outletDetailsService = SpringContext.getBean(OutletDetailsService.class);

    public RuleResult apply(CkOutletDetails cdm) {
        CkOutletDetails dbRecord = outletDetailsService.findByOutletCode(cdm.getOutletcode());

        if(cdm.getOutletcode()=="auto_generated"){
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
