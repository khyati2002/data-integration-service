/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.salescode.channelkart.validations.impl;


import com.salescode.channelkart.utils.StringUtils;
import com.salescode.dataintegration.etl.validation.AbstractValidationRule;
import com.salescode.dataintegration.etl.validation.RuleResult;
import com.salescode.dataintegration.etl.validation.ValidationResult;
import com.salescode.jooq.generated.tables.pojos.CkOutletDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OutletDetailsValidator extends AbstractValidationRule<CkOutletDetails> {
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
	public RuleResult apply(CkOutletDetails cdm) {
	//	return TimerUtils.withTime("Time taken to validate outlet "+cdm.getOutletCode(),k->{

		List<String> errors= new ArrayList<>();
		Set<ConstraintViolation<CkOutletDetails>> constraintViolations=validator.validate(cdm);
		for (ConstraintViolation<CkOutletDetails> violation : constraintViolations) {
			errors.add(StringUtils.format("'{}' : {}",violation.getPropertyPath().toString(),violation.getMessage()));
		}
		if(!errors.isEmpty()){
			String errorstr= StringUtils.format("Error saving outlet : {}, Reason : [{}]", cdm.getOutletcode(),org.apache.commons.lang.StringUtils.join(errors, ","));
			logger.error(errorstr);
			return new RuleResult(ValidationResult.Status.ERROR,errorstr);
		}
		return  RuleResult.OK;
	//	});

	}

}
