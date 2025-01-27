package com.applicate.unnati.validation;

import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.validations.AbstractRule;
import com.applicate.services.channelkart.validations.RuleResult;
import com.applicate.services.channelkart.validations.Status;
import com.applicate.services.channelkart.validations.repository.RegexValidation;

public class UserValidatorITCL extends AbstractRule<User> {

    final UserService userservice = (UserService) SpringContext.getBean(UserService.class);

    @Override
    public RuleResult apply(User cdm) {
        StringBuilder ruleResult = new StringBuilder();
        RegexValidation regexValidation = new RegexValidation();
        String regexNum = "^[0-9]*";
        String regexY_N = "^(Y|N)$";
        if(cdm.getDesignation().contains("supplier")) {
            if (!cdm.getSupplierMetaData().isEmpty()) {
                if (cdm.getSupplierMetaData().get(0).getExtendedAttributes().has("orderFunction")
                        && cdm.getSupplierMetaData().get(0).getExtendedAttributes().has("orderFulfillmentTime")) {
                    if (!(regexValidation.match(regexY_N,
                            cdm.getSupplierMetaData().get(0).getExtendedAttributes().get("orderFunction").asText()))) {
                        ruleResult.append("Field OrderFunc must have 'Y' or 'N' only");
                        return new RuleResult(Status.ERROR, ruleResult.toString());

                    }
                    if (!(regexValidation.match(regexNum, cdm.getSupplierMetaData().get(0).getExtendedAttributes()
                            .get("orderFulfillmentTime").asText()))) {
                        ruleResult.append(
                                "Field OrderFulfillmentTime is not matching our requirements. It contains some special characters except this [ 0-9 ]");
                        return new RuleResult(Status.ERROR, ruleResult.toString());

                    }
                    if (!(regexValidation.match(regexNum, String.valueOf(cdm.getSupplierMetaData().get(0).getMin())))) {
                        ruleResult.append(
                                "Field MinOrderValue is not matching our requirements. It contains some special characters except this [ 0-9 ]");
                        return new RuleResult(Status.ERROR, ruleResult.toString());
                    }
                } else {
                    return new RuleResult(Status.ERROR, "Header mismatch check whether all the headers are present");
                }
            } else {
                return new RuleResult(Status.ERROR, "Header mismatch check whether all the headers are present");
            }
        }
        return RuleResult.OK;
    }

}
