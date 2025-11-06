/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.validations.repository;

import com.applicate.services.channelkart.exceptions.ValidationException;
import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.jooq.impl.User;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;

public class UserEntityValidator extends AbstractValidationRule<User> {

	@Override
	public OperationResult.StepResult apply(User cdm) {
		Set<String> failedProperty = new HashSet<>();
		FormValidator entityValidation = FormValidator.get();
		try {
			if (cdm == null) {
				throw new IllegalArgumentException("Invalid 'null' argument passed in entity validator");
			}
			if (StringUtils.isNullOrBlank(cdm.getLoginId()) || StringUtils.isNullOrBlank(cdm.getUserAccountId())) {
				throw new ValidationException("'loginId' or 'userAccountId' must not be empty");
			}
			List<String> errors = new ArrayList<>();
			List<String> ignore = List.of("loginId", "userAccountId");
			Set<ConstraintViolation<CommonDataModel>> constraintViolations = entityValidation.formValidation(cdm);
			List<String> fieldsList = entityValidation.getConstraintsForClass(User.class);
			constraintViolations.stream().forEach(cv -> {
				errors.add(StringUtils.format("'{}' : {}", cv.getPropertyPath().toString(), cv.getMessage()));
				failedProperty.add(cv.getPropertyPath().toString());
			});

			if (fieldsList.size() == failedProperty.size()) {
				throw new ValidationException(
						"All entity constraints failing. Invalid object. Reason: [{}]",
						String.join(",", errors));
			}
			fieldsList.removeAll(ignore);
			failedProperty.removeAll(ignore);
			if (fieldsList.size() == failedProperty.size()) {
				throw new ValidationException(
						"All entity constraints failing except 'loginId' or 'userAccountId'. Invalid data.",
						String.join(",", errors)
				);
			}

			if (!errors.isEmpty()) {
				return new OperationResult.StepResult(OperationResult.Status.ERROR, StringUtils.format("User : {} (Cannot store user or it doesnt exist in database), Reason : [{}]",cdm.getLoginId(), String.join(",", errors)));
			}
		} catch (Exception ex) {
			return new OperationResult.StepResult(OperationResult.Status.ERROR, ex.getLocalizedMessage());
		}

		return OperationResult.StepResult.OK;

	}
}
