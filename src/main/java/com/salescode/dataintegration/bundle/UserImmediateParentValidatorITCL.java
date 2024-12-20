package com.salescode.dataintegration.bundle;


import com.github.jknack.handlebars.internal.lang3.StringUtils;
import com.salescode.channelkart.models.HierarchyMetaData;
import com.salescode.channelkart.models.User;
import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.services.UserService;
import com.salescode.channelkart.validations.repository.RegexValidation;
import com.salescode.dataintegration.etl.validation.AbstractValidationRule;
import com.salescode.dataintegration.etl.validation.RuleResult;
import com.salescode.dataintegration.etl.validation.ValidationResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserImmediateParentValidatorITCL extends AbstractValidationRule<User> {

    private UserService userService =  SpringContext.getBean(UserService.class);
    private static final String CAPITAL_CASE_REGEX = "(^[A-Z]*$)";
    private static final String IMMEDIATE_PARENT_MESSAGE_1 = "immediate parent can not be null.";
    private static final String IMMEDIATE_PARENT_MESSAGE_2 = "Given immediate parent is not present in database.Please verify the input data.";
    private static final String IMMEDIATE_PARENT_MESSAGE_3 = "Given immediate parent is not active any more.Please verify the input data.";
    private static final String IMMEDIATE_PARENT_MESSAGE_4 = "District and Branch value of loginId is not matching with Supplier's location.Please verify the input data.";


    @Override
    public RuleResult apply(User cdm) {
        RegexValidation regexValidation = new RegexValidation();
        Set<String> ruleResult = new HashSet<>();
        List<HierarchyMetaData> parentList = cdm.getImmediateParent();

        if (checkIfValidationRequiredForUserDesignation(cdm)) {
            if (parentList == null || parentList.isEmpty()) {
                return new RuleResult(ValidationResult.Status.ERROR, IMMEDIATE_PARENT_MESSAGE_1.concat(" for user ").concat(cdm.getLoginId()));
            }
            parentListDataCheck(cdm,parentList,ruleResult);
            branchInfoValidation(regexValidation,cdm,ruleResult);
            districtInfoValidation(regexValidation,cdm,ruleResult);
        }

        if (!ruleResult.isEmpty() ) {
            return new RuleResult(ValidationResult.Status.ERROR, org.apache.commons.lang.StringUtils.join(ruleResult, ", "));
        } else {
            return RuleResult.OK;
        }
    }

    private void districtInfoValidation(RegexValidation regexValidation, User cdm, Set<String> ruleResult) {
        if (cdm.getLocationHierarchy().getDistrict() != null) {
            if (!regexValidation.match(CAPITAL_CASE_REGEX, cdm.getLocationHierarchy().getDistrict())) {
                ruleResult.add(
                        "Value given for district should contain only alphabets with capital case.Current given value is not compatible");
            }
        } else {
            ruleResult.add("District can not be null");
        }
    }

    private void branchInfoValidation(RegexValidation regexValidation, User cdm, Set<String> ruleResult) {
        if (cdm.getLocationHierarchy().getBranch() != null) {
            if (!regexValidation.match(CAPITAL_CASE_REGEX, cdm.getLocationHierarchy().getBranch())) {
                ruleResult.add(
                        "Value given for branch should contain only alphabets with capital case.Current given value is not compatible");
            }
        } else {
            ruleResult.add("Branch can not be null");
        }
    }

    private void parentListDataCheck(User cdm, List<HierarchyMetaData> parentList, Set<String> ruleResult) {
        parentList.forEach(parentID -> {
            if (parentID == null) {
                ruleResult.add(IMMEDIATE_PARENT_MESSAGE_1);
            }
            User parent = userService.findByLoginId(parentID.getImmediateParent());
            if (parent == null) {
                ruleResult.add(IMMEDIATE_PARENT_MESSAGE_2);
            }

            if ((parent.getActiveStatus() != ActiveStatus.ACTIVE)) {
                ruleResult.add(IMMEDIATE_PARENT_MESSAGE_3);
            }

            if (cdm.getDesignation().contains("retailer")
                    && (!StringUtils.equals(parent.getLocationHierarchy().getBranch(), cdm.getLocationHierarchy().getBranch())
                    || !StringUtils.equals(parent.getLocationHierarchy().getDistrict(), cdm.getLocationHierarchy().getDistrict()))) {
                ruleResult.add(IMMEDIATE_PARENT_MESSAGE_4);
            }
        });
    }

    private boolean checkIfValidationRequiredForUserDesignation(User cdm) {
        return (cdm.getDesignation().contains("retailer") || cdm.getDesignation().contains("supplier"));
    }
}