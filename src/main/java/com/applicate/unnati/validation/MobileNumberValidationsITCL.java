package com.applicate.unnati.validation;

import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.applicate.services.channelkart.validations.AbstractRule;
import com.applicate.services.channelkart.validations.RuleResult;
import com.applicate.services.channelkart.validations.Status;
import com.applicate.services.channelkart.validations.repository.RegexValidation;

import java.util.List;
import java.util.Optional;

public class MobileNumberValidationsITCL extends AbstractRule<User> {

    private final UserService userService;

    public MobileNumberValidationsITCL() {
        this.userService= SpringContext.getBean(UserService.class);
    }

    String regex = "(^[0-9]{10}$)";

    @Override
    public RuleResult apply(User cdm) {
        StringBuilder ruleResult = new StringBuilder();

        if (checkDesignation(cdm)) {
                    ruleResult.append("Given mobile number of the user did not match the required validations.Mobile number should exactly contain 10 numeric values.");
                    return new RuleResult(Status.ERROR, ruleResult.toString());
        }

        return RuleResult.OK;
    }

    public boolean checkDesignation(User cdm){
        return ((cdm.getDesignation().contains("supplier") || cdm.getDesignation().contains("branch")) && cdm.getMobile() != null && (!checkMobileNumberPattern(cdm.getMobile())));
    }

    public boolean checkMobileNumberPattern(String mobile) {
        RegexValidation regexValidation = new RegexValidation();
        return regexValidation.match(regex, mobile);

    }



}
