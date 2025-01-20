/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.validations.repository;


import com.applicate.services.channelkart.models.OutletDetails;
import com.applicate.services.channelkart.utils.StringUtils;
import com.applicate.services.channelkart.validations.AbstractRule;
import com.applicate.services.channelkart.validations.RuleResult;
import com.applicate.services.channelkart.validations.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OutletDetailsValidator extends AbstractRule<OutletDetails> {
	/** The factory. */
	private ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
	
	/** The validator. */
	private Validator validator = factory.getValidator();
	
	/** The logger. */
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	/**
	 * Apply.
	 *
	 * @param cdm the cdm
	 * @return the rule result
	 */
	@Override
	public RuleResult apply(OutletDetails cdm) {
	//	return TimerUtils.withTime("Time taken to validate outlet "+cdm.getOutletCode(),k->{

		List<String> errors= new ArrayList<>();
		Set<ConstraintViolation<OutletDetails>> constraintViolations=validator.validate(cdm);
		for (ConstraintViolation<OutletDetails> violation : constraintViolations) {
			errors.add(StringUtils.format("'{}' : {}",violation.getPropertyPath().toString(),violation.getMessage()));
		}
		if(!errors.isEmpty()){
			String errorstr= StringUtils.format("Error saving outlet : {}, Reason : [{}]", cdm.getOutletCode(),org.apache.commons.lang.StringUtils.join(errors, ","));
			logger.error(errorstr);
			return new RuleResult(Status.ERROR,errorstr);
		}
		return  RuleResult.OK;
	//	});

	}

}
