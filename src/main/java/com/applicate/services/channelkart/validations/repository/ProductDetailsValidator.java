package com.applicate.services.channelkart.validations.repository;

import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.ProductDetails;

public class ProductDetailsValidator extends AbstractValidationRule<ProductDetails> {

    @Override
    public OperationResult.StepResult apply(ProductDetails cdm) {
        return OperationResult.StepResult.OK;
    }
}