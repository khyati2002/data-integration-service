package com.applicate.unnati.validation;


import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.validations.repository.RegexValidation;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.User;

public class UserValidatorITCL extends AbstractValidationRule<User> {

    final UserService userservice = (UserService) ServiceLocator.lookup(User.class);

    @Override
    public OperationResult.StepResult apply(User cdm) {
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
                        return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());

                    }
                    if (!(regexValidation.match(regexNum, cdm.getSupplierMetaData().get(0).getExtendedAttributes()
                            .get("orderFulfillmentTime").asText()))) {
                        ruleResult.append(
                                "Field OrderFulfillmentTime is not matching our requirements. It contains some special characters except this [ 0-9 ]");
                        return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());

                    }
                    if (!(regexValidation.match(regexNum, String.valueOf(cdm.getSupplierMetaData().get(0).getMin())))) {
                        ruleResult.append(
                                "Field MinOrderValue is not matching our requirements. It contains some special characters except this [ 0-9 ]");
                        return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
                    }
                } else {
                    return new OperationResult.StepResult(OperationResult.Status.ERROR, "Header mismatch check whether all the headers are present");
                }
            } else {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, "Header mismatch check whether all the headers are present");
            }
        }
        return OperationResult.StepResult.OK;
    }

}
