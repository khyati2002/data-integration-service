package com.salescode.dataintegration.bundle;


import com.salescode.channelkart.models.OutletDetails;
import com.salescode.channelkart.validations.AbstractRule;
import com.salescode.channelkart.validations.RuleResult;
import com.salescode.channelkart.validations.Status;
import com.salescode.channelkart.validations.repository.RegexValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

public class OutletDetailsValidatorITCL extends AbstractRule<OutletDetails> {
    /** The logger. */
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Apply.
     *
     * @param cdm the cdm
     * @return the rule result
     */
    @Override
    public RuleResult apply(OutletDetails cdm) {
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
            return new RuleResult(Status.ERROR,ruleResult.toString());
        }else {
            return RuleResult.OK;
        }
    }
}