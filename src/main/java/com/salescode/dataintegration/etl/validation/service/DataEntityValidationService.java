package com.salescode.dataintegration.etl.validation.service;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.utils.StringUtils;
import com.salescode.dataintegration.etl.registry.ETLRegistry;
import com.salescode.dataintegration.etl.validation.FormValidator;
import com.salescode.dataintegration.etl.validation.RuleResult;
import com.salescode.dataintegration.etl.validation.ValidationResult;
import com.salescode.dataintegration.etl.validation.registry.ValidationInfoRegistry;
import com.salescode.jooq.generated.tables.pojos.CkValidationRule;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DataEntityValidationService extends DataValidationService {

    private static final String PREFIX = "ev-{}";
    private final FormValidator formValidator;

    @Autowired
    public DataEntityValidationService(ValidationInfoRegistry validationInfoRegistry, ETLRegistry etlRegistry, FormValidator formValidator) {
        super(validationInfoRegistry, etlRegistry);
        this.formValidator = formValidator;
    }

    /**
     * Validates the given CommonDataModel and returns a ValidationResult.
     *
     * @param currentDataModels the CommonDataModel to validate
     * @return the result of the validation
     */
    public ValidationResult validate(List<CommonDataModel> currentDataModels) {
        if (currentDataModels == null || currentDataModels.isEmpty()) {
            return new ValidationResult(ValidationResult.Status.OK, Collections.emptyList());
        }
        List<RuleResult> allValidationResults = new ArrayList<>();
        List<CkValidationRule> validationRules = fetchValidationRules(StringUtils.format(PREFIX, currentDataModels.get(0).getClass().getSimpleName()));
        for (CkValidationRule validationRule : validationRules) {
            List<RuleResult> validationResults = applyRuleToDataModels(validationRule, currentDataModels);
            allValidationResults.addAll(validationResults);
        }
        return (ObjectUtils.isEmpty(allValidationResults)) ? defaultValidator(currentDataModels) : evaluateResults(allValidationResults);
    }


    /**
     * Default validator using form constraints when no rule-based validation is applied.
     *
     * @param commonDataModels the CommonDataModel to validate
     * @return the result of the default validation
     */
    private <T> ValidationResult defaultValidator(List<T> commonDataModels) {
        List<RuleResult> errors = new ArrayList<>();
        Set<ConstraintViolation<T>> constraintViolations = commonDataModels.stream().flatMap(s -> formValidator.validate(s).stream()).collect(Collectors.toSet());
        for (ConstraintViolation<T> violation : constraintViolations) {
            errors.add(new RuleResult(ValidationResult.Status.ERROR, StringUtils.format("Field: '{}': {}", violation.getPropertyPath().toString(), violation.getMessage())));
        }
        return evaluateResults(errors);
    }
}