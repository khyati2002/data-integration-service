package com.applicate.unnati.validation;

import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.ProductDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.utils.StringUtils;

import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.generated.tables.pojos.RecommendedOrder;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.ProductDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class IncentiveRecommendedOrdersValidatorITCL extends AbstractValidationRule<RecommendedOrder> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String BILLED_SKU="BilledSKU";


    @Override
    public OperationResult.StepResult apply(RecommendedOrder cdm) {
        final OutletDetailsService outletDetailsService = (OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);
        final ProductDetailsService productDetailsService = (ProductDetailsService) ServiceLocator.lookup(ProductDetails.class);

        List<String> errors = new ArrayList<>();
        boolean productExists = false;

        if (NullUtils.isNotNull(cdm.getBatchCode())) {
            productExists = productDetailsService.checkIfBatchCodeExists(cdm.getBatchCode());
        } else {
            errors.add("Product sku code can't be null.");
        }

        if (!productExists) {
            errors.add("Product with sku code is not present in the database.");
        }

        if (NullUtils.isNull(cdm.getOutletcode())) {
            errors.add("Outlet code can't be null.");
        } else {
            OutletDetails dbRecord =  outletDetailsService
                    .findByOutletCode(cdm.getOutletcode());
            if (dbRecord == null) {
                errors.add("OutletCode is not present in OutletDetails");
            }
        }

        if(!StringUtils.isNullOrBlank(cdm.getSupportkpi()) && BILLED_SKU.equalsIgnoreCase(cdm.getSupportkpi()) && NullUtils.isNull(cdm.getExtendedAttributes())){
            errors.add("Record is not present in prediction data.");
        }

        if (errors.isEmpty()) {
            return OperationResult.StepResult.OK;
        }else{
            String errorstr = StringUtils.format(
                    "Validation error occurred for Recommendation. Kindly go through provided errors and make sure those conditions should fulfill while retrying. {}",
                    String.join( ", ",errors));
            logger.error(errorstr);
            return new OperationResult.StepResult(OperationResult.Status.ERROR, errorstr);
        }

    }

}

