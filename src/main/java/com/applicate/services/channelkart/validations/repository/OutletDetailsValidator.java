package com.applicate.services.channelkart.validations.repository;

import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.OutletDetails;

import java.util.ArrayList;
import java.util.List;

public class OutletDetailsValidator extends AbstractValidationRule<OutletDetails> {

	@Override
	public OperationResult.StepResult apply(OutletDetails outlet) {

		List<String> errors = new ArrayList<>();

		if (isBlank(outlet.getOutletcode())) {
			errors.add("'outletcode' : must not be null or empty");
		}

		if (outlet.getVersion() == null) {
			errors.add("'version' : must not be null");
		}

		if (outlet.getMapped() == null) {
			errors.add("'mapped' : must not be null");
		}

		if (!errors.isEmpty()) {
			String errorStr = StringUtils.format(
					"Error saving outlet : {}, Reason : [{}]",
					outlet.getOutletcode(),
					org.apache.commons.lang3.StringUtils.join(errors, ',')
			);
			return new OperationResult.StepResult(OperationResult.Status.ERROR, errorStr);
		}

		return OperationResult.StepResult.OK;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
