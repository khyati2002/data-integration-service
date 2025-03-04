package com.applicate.unnati.validation;

import com.applicate.services.channelkart.validations.repository.RegexValidation;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutletDetailsValidatorITCL extends AbstractValidationRule<OutletDetails> {
    /** The logger. */
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Apply.
     *
     * @param cdm the cdm
     * @return the rule result
     */
    @Override
    public OperationResult.StepResult apply(OutletDetails cdm) {
        RegexValidation regexValidation = new RegexValidation();
        String regexY_N = "^(Y|N)$";
        String regexLoyaltyFlag = "^(loyalty|non loyalty)$";
        StringBuilder ruleResult = new StringBuilder();
        if(cdm.getExtendedAttributes()!=null){
            if(cdm.getExtendedAttributes().has("custOrder")) {
                if(!regexValidation.match(regexY_N, cdm.getExtendedAttributes().get("custOrder").asText())) {
                    ruleResult.append("Order can have 'Y' or 'N' only");
                }
            }else {
                ruleResult.append("Order cannot not be null");
            }

            if(cdm.getExtendedAttributes().has("custLoyalty")) {
                if(!regexValidation.match(regexY_N, cdm.getExtendedAttributes().get("custLoyalty").asText())) {
                    ruleResult.append("Loyalty can have 'Y' or 'N' only");
                }
            }else {
                ruleResult.append("Loyalty cannot be null");
            }
        }


        if(null!=cdm.getOutletCategory()) {
            if(regexValidation.match(regexLoyaltyFlag, cdm.getOutletCategory())) {
                if(cdm.getOutletCategory().equalsIgnoreCase("non loyalty") && cdm.getExtendedAttributes().get("custLoyalty").asText().equals("Y")) {
                    ruleResult.append("Loyalty value cannot be Y for non loyalty outlets");
                }
            }else {
                ruleResult.append("Unexpected value found in loyaltyFlag. LoyaltyFlag can have either  be loyalty or non loyalty");
            }
        }else {
            ruleResult.append("LoyaltyFlag cannot be null or empty");
        }

        if(ruleResult.length()>0) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR,ruleResult.toString());
        }else {
            return OperationResult.StepResult.OK;
        }
    }
}