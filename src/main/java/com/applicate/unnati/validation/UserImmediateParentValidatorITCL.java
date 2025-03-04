package com.applicate.unnati.validation;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.validations.repository.RegexValidation;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserImmediateParentValidatorITCL extends AbstractValidationRule<User> {

    private UserService userService = (UserService) ServiceLocator.lookup(User.class);
    private static final String CAPITAL_CASE_REGEX = "(^[A-Z]*$)";
    private static final String IMMEDIATE_PARENT_MESSAGE_1 = "immediate parent can not be null.";
    private static final String IMMEDIATE_PARENT_MESSAGE_2 = "Given immediate parent is not present in database.Please verify the input data.";
    private static final String IMMEDIATE_PARENT_MESSAGE_3 = "Given immediate parent is not active any more.Please verify the input data.";
    private static final String IMMEDIATE_PARENT_MESSAGE_4 = "District and Branch value of loginId is not matching with Supplier's location.Please verify the input data.";


    @Override
    public OperationResult.StepResult apply(User cdm) {
        RegexValidation regexValidation = new RegexValidation();
        Set<String> ruleResult = new HashSet<>();
        List<HierarchyMetadata> parentList = cdm.getImmediateParent();

        if (checkIfValidationRequiredForUserDesignation(cdm)) {
            if (parentList == null || parentList.isEmpty()) {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, IMMEDIATE_PARENT_MESSAGE_1.concat(" for user ").concat(cdm.getLoginid()));
            }
            parentListDataCheck(cdm,parentList,ruleResult);
            branchInfoValidation(regexValidation,cdm,ruleResult);
            districtInfoValidation(regexValidation,cdm,ruleResult);
        }

        if (!ruleResult.isEmpty() ) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, org.apache.commons.lang3.StringUtils.join(ruleResult, ", "));
        } else {
            return OperationResult.StepResult.OK;
        }
    }

    private void districtInfoValidation(RegexValidation regexValidation, User cdm, Set<String> ruleResult) {
        if (cdm.getLocation().getDistrict() != null) {
            if (!regexValidation.match(CAPITAL_CASE_REGEX, cdm.getLocation().getDistrict())) {
                ruleResult.add(
                        "Value given for district should contain only alphabets with capital case.Current given value is not compatible");
            }
        } else {
            ruleResult.add("District can not be null");
        }
    }

    private void branchInfoValidation(RegexValidation regexValidation, User cdm, Set<String> ruleResult) {
        if (cdm.getLocation().getBranch() != null) {
            if (!regexValidation.match(CAPITAL_CASE_REGEX, cdm.getLocation().getBranch())) {
                ruleResult.add(
                        "Value given for branch should contain only alphabets with capital case.Current given value is not compatible");
            }
        } else {
            ruleResult.add("Branch can not be null");
        }
    }

    private void parentListDataCheck(User cdm, List<HierarchyMetadata> parentList, Set<String> ruleResult) {
        parentList.forEach(parentID -> {
            if (parentID == null) {
                ruleResult.add(IMMEDIATE_PARENT_MESSAGE_1);
            }
            User parent = userService.findByLoginId(parentID.getParent());
            if (parent == null) {
                ruleResult.add(IMMEDIATE_PARENT_MESSAGE_2);
                return;
            }

            if ((parent.getActiveStatus() != ActiveStatus.ACTIVE)) {
                ruleResult.add(IMMEDIATE_PARENT_MESSAGE_3);
            }

            if (cdm.getDesignation().contains("retailer")
                    && (!StringUtils.equals(parent.getLocation().getBranch(), cdm.getLocation().getBranch())
                    || !StringUtils.equals(parent.getLocation().getDistrict(), cdm.getLocation().getDistrict()))) {
                ruleResult.add(IMMEDIATE_PARENT_MESSAGE_4);
            }
        });
    }

    private boolean checkIfValidationRequiredForUserDesignation(User cdm) {
        return (cdm.getDesignation().contains("retailer") || cdm.getDesignation().contains("supplier"));
    }
}