package com.salescode.dataintegration.etl.validation.service;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.dataintegration.etl.registry.ETLRegistry;
import com.salescode.dataintegration.etl.validation.AbstractValidationRule;
import com.salescode.dataintegration.etl.validation.RuleResult;
import com.salescode.dataintegration.etl.validation.ValidationResult;
import com.salescode.dataintegration.etl.validation.registry.ValidationInfoRegistry;
import com.salescode.channelkart.validations.RuleInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataValidationService {

    private static final String ERROR_MESSAGE = "An error occurred while validating input";

    private final ETLRegistry etlRegistry;
    private final ValidationInfoRegistry validationInfoRegistry;


    @Autowired
    public DataValidationService(ValidationInfoRegistry validationInfoRegistry, ETLRegistry etlRegistry) {
        this.validationInfoRegistry = validationInfoRegistry;
        this.etlRegistry = etlRegistry;
    }

    public ValidationResult validate(List<CommonDataModel> currentDataModels) {
        if (currentDataModels == null || currentDataModels.isEmpty()) {
            return new ValidationResult(ValidationResult.Status.OK, Collections.emptyList());
        }
        List<RuleResult> allValidationResults = new ArrayList<>();
        List<RuleInfo> validationRules = fetchValidationRules(currentDataModels.get(0).getClass().getSimpleName());
        for (RuleInfo validationRule : validationRules) {
            List<RuleResult> validationResults = applyRuleToDataModels(validationRule, currentDataModels);
            allValidationResults.addAll(validationResults);
        }
        return evaluateResults(allValidationResults);
    }


    /**
     * Evaluates the overall validation results to produce a final ValidationResult.
     *
     * @param ruleResults the list of rule results to evaluate
     * @return the aggregated ValidationResult
     */
    protected ValidationResult evaluateResults(List<RuleResult> ruleResults) {
        Map<ValidationResult.Status, List<RuleResult>> resultsByStatus = ruleResults.stream()
                .collect(Collectors.groupingBy(RuleResult::getStatus));

        ValidationResult.Status finalStatus = determineFinalStatus(resultsByStatus);

        ValidationResult validationResult = new ValidationResult(finalStatus);
        validationResult.getViolations().addAll(resultsByStatus.getOrDefault(ValidationResult.Status.ERROR, Collections.emptyList()));
        validationResult.getViolations().addAll(resultsByStatus.getOrDefault(ValidationResult.Status.WARNING, Collections.emptyList()));
        validationResult.getViolations().addAll(resultsByStatus.getOrDefault(ValidationResult.Status.CONFLICT, Collections.emptyList()));

        return validationResult;
    }

    /**
     * Determines the final status based on the presence of different statuses in the results.
     *
     * @param resultsByStatus a map of statuses to their corresponding results
     * @return the final validation status
     */
    private ValidationResult.Status determineFinalStatus(Map<ValidationResult.Status, List<RuleResult>> resultsByStatus) {
        if (resultsByStatus.containsKey(ValidationResult.Status.ERROR)) {
            return ValidationResult.Status.ERROR;
        } else if (resultsByStatus.containsKey(ValidationResult.Status.CONFLICT)) {
            return ValidationResult.Status.CONFLICT;
        } else if (resultsByStatus.containsKey(ValidationResult.Status.WARNING)) {
            return ValidationResult.Status.WARNING;
        } else {
            return ValidationResult.Status.OK;
        }
    }


    /**
     * Applies a single validation rule to a list of data models.
     *
     * @param rule       the validation rule
     * @param dataModels the data models to validate
     * @return a list of validation results
     */
    protected List<RuleResult> applyRuleToDataModels(RuleInfo rule, List<CommonDataModel> dataModels) {
        return dataModels.stream()
                .map(model -> applyValidation(model, rule))
                .collect(Collectors.toList());
    }

    /**
     * Applies a single validation to a data model.
     *
     * @param cdm            the data model
     * @param validationRule the validation rule
     * @return the result of the validation
     */
    private RuleResult applyValidation(CommonDataModel cdm, RuleInfo validationRule) {
        try {
            AbstractValidationRule<CommonDataModel> validation = etlRegistry.getValidationRule(validationRule.getImplementation());
            validation.setValidationRule(validationRule);
            return validation.apply(cdm);
        } catch (Exception e) {
            return new RuleResult(ValidationResult.Status.ERROR, ERROR_MESSAGE + ": " + e.getMessage());
        }
    }


    /**
     * Fetches and sorts validation rules applicable to the specified type.
     *
     * @param type the validation type
     * @return a list of sorted validation rules
     */
    protected List<RuleInfo> fetchValidationRules(String type) {
        return validationInfoRegistry.getValidationRulesByType(type)
                .stream()
                .sorted(Comparator.comparingInt(RuleInfo::getPriority))
                .collect(Collectors.toList());
    }
}