package com.applicate.services.channelkart.validations.repository;

import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.generated.tables.pojos.OutletDetails;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.validation.ValidatorFactory;

public class OutletDetailsValidator extends AbstractValidationRule<OutletDetails>{
    /** The factory. */


    /** The logger. */

    /**
     * Apply.
     *
     * @param cdm the cdm
     * @return the rule result
     */
    @Override
    public OperationResult.StepResult apply(OutletDetails cdm) {

         ValidatorFactory factory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator()) // Use simple interpolator
                .buildValidatorFactory();

        /** The validator. */
          Validator validator = factory.getValidator();
            List<String> errors= new ArrayList<>();
            Set<ConstraintViolation<OutletDetails>> constraintViolations=validator.validate(cdm);
            for (ConstraintViolation<OutletDetails> violation : constraintViolations) {
                errors.add(StringUtils.format("'{}' : {}",violation.getPropertyPath().toString(),violation.getMessage()));
            }
            if(!errors.isEmpty()){
                String errorstr= StringUtils.format("Error saving outlet : {}, Reason : [{}]", cdm.getOutletcode(),org.apache.commons.lang3.StringUtils.join(errors, ","));
            //    logger.error(errorstr);
                return new OperationResult.StepResult(OperationResult.Status.ERROR,errorstr);
            }
            return OperationResult.StepResult.OK;
        }



}
