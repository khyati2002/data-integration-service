package com.applicate.services.channelkart.validations.repository;

import com.applicate.services.channelkart.exceptions.ValidationException;

import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.jooq.impl.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class UserEntityValidator extends AbstractValidationRule<User> {

	@Override
	public OperationResult.StepResult apply(User cdm) {
		Set<String> failedProperty = new HashSet<>();
		List<String> errors = new ArrayList<>();
		List<String> ignore = List.of("loginId", "userAccountId");

		try {
			if (cdm == null) {
				throw new IllegalArgumentException("Invalid 'null' argument passed in entity validator");
			}

			if (StringUtils.isNullOrBlank(cdm.getLoginid()) || StringUtils.isNullOrBlank(cdm.getUseraccountid())) {
				throw new ValidationException("'loginId' or 'userAccountId' must not be empty");
			}

			Set<Map.Entry<String, String>> constraintViolations = simulateValidation(cdm);

			List<String> fieldsList = getFieldsWithConstraints(User.class);

			for (Map.Entry<String, String> cv : constraintViolations) {
				errors.add(StringUtils.format("'{}' : {}", cv.getKey(), cv.getValue()));
				failedProperty.add(cv.getKey());
			}

			if (fieldsList.size() == failedProperty.size()) {
				throw new ValidationException(
						"All entity constraints failing. Invalid object. Reason: [{}]",
						String.join(",", errors)
				);
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
				return new OperationResult.StepResult(
						OperationResult.Status.ERROR,
						StringUtils.format(
								"User : {} (Cannot store user or it doesnt exist in database), Reason : [{}]",
								cdm.getLoginid(),
								String.join(",", errors)
						)
				);
			}

		} catch (Exception ex) {
			return new OperationResult.StepResult(OperationResult.Status.ERROR, ex.getLocalizedMessage());
		}

		return OperationResult.StepResult.OK;
	}

	/**
	 * Mimics FormValidator.getConstraintsForClass(User.class)
	 * by listing all declared field names.
	 */
	private List<String> getFieldsWithConstraints(Class<?> clazz) {
		List<String> fields = new ArrayList<>();
		Arrays.stream(clazz.getDeclaredFields()).forEach(field -> fields.add(field.getName()));
		return fields;
	}
	
	private Set<Map.Entry<String, String>> simulateValidation(User cdm) {
		return new HashSet<>();
	}
}
