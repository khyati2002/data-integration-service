package com.applicate.services.channelkart.validations.repository;

import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.SchemeDefination;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneOffset;
import java.util.Date;

@Slf4j
public class SchemeDTMarginValidation extends AbstractValidationRule<SchemeDefination> {

    @Override
    public OperationResult.StepResult apply(SchemeDefination scheme) {
        if (NullUtils.isNotNull(scheme.getProgramLevel()) &&
            scheme.getProgramLevel().equalsIgnoreCase("dt_discount") &&
            isEndDateExpired(Date.from(scheme.getEndDate().toInstant(ZoneOffset.of("UTC"))))) {

            String error = "End Date is before the Current Date while saving the DT discount object";
            log.error(error);
            return OperationResult.StepResult.ERROR;
        }
        return OperationResult.StepResult.OK;
    }

    private boolean isEndDateExpired(Date endDate) {
        return NullUtils.isNotNull(endDate) && endDate.before(new Date());
    }
}
