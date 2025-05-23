package com.applicate.services.channelkart.enrichments.repository;


import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.ProductDetails;

public class ProductDetailsEnrichment extends AbstractEnrichment<ProductDetails> {

    @Override
    public EnrichmentResult apply(ProductDetails cdm) {
        if(StringUtils.isEmpty(cdm.getSuggestionText())){
            cdm.setSuggestionText(cdm.getSkuCode() + " " + cdm.getSkuDescription());
        }

        if(StringUtils.isEmpty(cdm.getBatchCode())){
            cdm.setBatchCode(cdm.getSkuCode());
        }

		return new OperationResult.StepResult(OperationResult.Status.OK, "Data enriched successfully");
	}

}
