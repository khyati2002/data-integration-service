package com.salescode.dim.etl.validation.service;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.ValidationResult;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.generated.tables.pojos.ValidationRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.meta.derby.sys.Sys;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.etl.OperationServiceHelper.getOperationResult;

/**
 * Service responsible for validating data models based on configured validation rules.
 * Applies rules in order of priority and manages the validation lifecycle.
 */
@Slf4j
public class DataValidationService {

    private static final String ERROR_MESSAGE = "Validation error: ";

    private final ValidationInfoRegistry validationInfoRegistry;
    private final ValidationExcludeGroupRegistry validationExcludeGroupRegistry;
    private final ETLRegistry etlRegistry;

    /**
     * Creates a new DataValidationService with the required dependencies.
     *
     * @param validationInfoRegistry         registry containing validation rule information
     * @param validationExcludeGroupRegistry
     * @param etlRegistry                    registry for obtaining validation implementations
     */
    public DataValidationService(ValidationInfoRegistry validationInfoRegistry, ValidationExcludeGroupRegistry validationExcludeGroupRegistry, ETLRegistry etlRegistry) {
        this.validationInfoRegistry = Objects.requireNonNull(validationInfoRegistry, "ValidationInfoRegistry cannot be null");
        this.validationExcludeGroupRegistry = Objects.requireNonNull(validationExcludeGroupRegistry, "ValidationExcludeGroupRegistry cannot be null");
        this.etlRegistry = Objects.requireNonNull(etlRegistry, "ETLRegistry cannot be null");
    }

    /**
     * Validates a single CommonDataModel.
     *
     * @param cdm the data model to validate (must not be null)
     * @return the result of the validation operation
     * @throws NullPointerException if cdm is null
     */
    public OperationResult validate(CommonDataModel cdm, String preprocessValidationExcludeGroup) {
        Objects.requireNonNull(cdm, "CommonDataModel cannot be null");
        return validate(Collections.singletonList(cdm), preprocessValidationExcludeGroup);
    }

    /**
     * Validates a list of CommonDataModels.
     * Applies all relevant validation rules in priority order.
     *
     * @param currentDataModels                the list of data models to validate
     * @param preprocessValidationExcludeGroup
     * @return the validation result containing validation status and violations
     */
    public OperationResult validate(List<CommonDataModel> currentDataModels, String preprocessValidationExcludeGroup) {
        if (currentDataModels == null || currentDataModels.isEmpty()) {
            return OperationResult.of(OperationResult.Status.OK, currentDataModels);
        }

        String modelType = currentDataModels.get(0).getClass().getSimpleName();
        List<ValidationRule> validationRules = fetchValidationRules(modelType);

        if (validationRules.isEmpty()) {
            return OperationResult.of(OperationResult.Status.OK, currentDataModels);
        }

        Set<String> excludedValidationsIds = StringUtils.isNotBlank(preprocessValidationExcludeGroup) ? validationExcludeGroupRegistry.getObjectIdListByKey(preprocessValidationExcludeGroup) : Collections.emptySet();

        List<ValidationResult> allResults = validationRules.parallelStream()
                .filter(rule -> !excludedValidationsIds.contains(rule.getId()))
                .map(rule -> validateWithRule(currentDataModels, rule))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        OperationResult result = evaluateResults(allResults);
        result.getOperationResultData().addAll(currentDataModels);
        return result;
    }

    private List<ValidationResult> validateWithRule(List<CommonDataModel> models, ValidationRule rule) {
        try {
            return models.parallelStream().map(model -> applyValidation(model, rule)).collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.singletonList(new OperationResult.StepResult(OperationResult.Status.ERROR, ERROR_MESSAGE + e.getMessage()));
        }
    }

    /**
     * Fetches and sorts validation rules applicable to the data model type.
     *
     * @param modelType the data model type (simple class name)
     * @return a list of sorted validation rules
     */
    private List<ValidationRule> fetchValidationRules(String modelType) {
        return validationInfoRegistry.getValidationRulesByType(modelType)
                .stream()
                .sorted(Comparator.comparingInt(ValidationRule::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * Applies a single validation to a data model.
     *
     * @param cdm            the data model
     * @param validationRule the validation rule
     * @return the result of the validation
     */
    private ValidationResult applyValidation(CommonDataModel cdm, ValidationRule validationRule) {
        long p1 = System.nanoTime();
        String implementationName = validationRule.getImplementation();
        try {
            AbstractValidationRule<CommonDataModel> validation = etlRegistry.getValidationRule(implementationName);
            validation.setValidationRule(validationRule);
            return validation.apply(cdm);
        } catch (Exception e) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, ERROR_MESSAGE + String.format("Implementation '%s' failed: %s", implementationName, e.getMessage()));
        } finally {
            long p2 = System.nanoTime();
//            log.info("CDM {} Validation {} took {} ns", cdm.getClass().getSimpleName(), validationRule.getImplementation(), p2 - p1);
        }
    }

    /**
     * Evaluates the overall status based on individual validation results.
     *
     * @param results the list of validation results
     * @return the validation result with aggregated status
     */
    private OperationResult evaluateResults(List<ValidationResult> results) {
        Map<OperationResult.Status, List<OperationResult.StepResult>> resultsByStatus = results.stream()
                .map(OperationResult.StepResult.class::cast)
                .collect(Collectors.groupingBy(ValidationResult::getStatus));

        return getOperationResult(resultsByStatus);
    }

}
