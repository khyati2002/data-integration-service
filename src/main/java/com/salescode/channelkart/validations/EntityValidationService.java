/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */

package com.salescode.channelkart.validations;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.utils.StringUtils;
import com.salescode.channelkart.validations.repository.FormValidator;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The class EntityValidationService.
 *
 * @author Manish Srivastava
 * @version 1.1
 * @since Dec 2020
 */
@Service
public class EntityValidationService {

    /**
     * The Constant prefix.
     */
    public static final String PREFIX = "ev-{}";
    /**
     * The entity validation.
     */
    private final FormValidator entityValidation;

    public EntityValidationService(FormValidator entityValidation) {
        this.entityValidation = entityValidation;
    }

    /**
     * Validate.
     *
     * @param cdm the cdm
     * @return the entity validation result
     */
    public EntityValidationResult validate(CommonDataModel cdm) {

        String lob = SecurityContextUtils.getLob();

        List<RuleInfo> rules = RuleRegistry.INSTANCE.get(lob, StringUtils.format(PREFIX, cdm.getClass().getSimpleName()));

        List<RuleResult> ruleResult = rules != null ? rules.stream().map(f -> RuleLogicEngine.INSTANCE.execute(cdm, f)).collect(Collectors.toList()) : new ArrayList<>();
        return (ObjectUtils.isEmpty(ruleResult)) ? defaultValidater(cdm) : evaluateResults(ruleResult);

    }

    /**
     * Evaluate results.
     *
     * @param ruleResult the rule result
     * @return the entity validation result
     */
    private EntityValidationResult evaluateResults(List<RuleResult> ruleResult) {

        Map<Status, List<RuleResult>> resultsByStatus = ruleResult.stream().collect(Collectors.groupingBy(RuleResult::getStatus));

        Status status = null;
        if (resultsByStatus.get(Status.ERROR) == null && resultsByStatus.get(Status.CONFLICT) == null) {
            status = Status.OK;
        } else {
            status = resultsByStatus.get(Status.CONFLICT) != null ? Status.CONFLICT : Status.ERROR;
        }

        EntityValidationResult vr = new EntityValidationResult(status);

        List<RuleResult> errorresult = resultsByStatus.get(Status.ERROR);
        if (!ObjectUtils.isEmpty(errorresult)) {
            vr.setMessage(org.apache.commons.lang3.StringUtils.join(errorresult.stream().filter(p -> StringUtils.isNotEmpty(p.getMessage())).map(RuleResult::getMessage).collect(Collectors.toList()), ","));
        }

        List<RuleResult> warnresult = resultsByStatus.get(Status.WARNING);
        if (!ObjectUtils.isEmpty(warnresult)) {
            vr.setMessage(org.apache.commons.lang3.StringUtils.join(warnresult.stream().filter(p -> StringUtils.isNotEmpty(p.getMessage())).map(RuleResult::getMessage).collect(Collectors.toList()), ","));
        }

        List<RuleResult> conflictresult = resultsByStatus.get(Status.CONFLICT);
        if (!ObjectUtils.isEmpty(conflictresult)) {
            vr.setMessage(org.apache.commons.lang3.StringUtils.join(conflictresult.stream().filter(p -> StringUtils.isNotEmpty(p.getMessage())).map(RuleResult::getMessage).collect(Collectors.toList()), ","));
        }

        return vr;
    }

    /**
     * Default validater.
     *
     * @param cdm the cdm
     * @return the entity validation result
     */
    @SuppressWarnings("rawtypes")
    public EntityValidationResult defaultValidater(Object cdm) {
        List<String> errors = new ArrayList<>();
        Set<ConstraintViolation<Object>> constraintViolations = entityValidation.formValidation(cdm);
        for (ConstraintViolation<Object> violation : constraintViolations) {
            errors.add(StringUtils.format("Field: '{}': {}", violation.getPropertyPath().toString(), violation.getMessage()));
        }
        if (!errors.isEmpty()) {
            return new EntityValidationResult(Status.ERROR, org.apache.commons.lang.StringUtils.join(errors, ","));
        }
        return EntityValidationResult.OK;
    }

}
