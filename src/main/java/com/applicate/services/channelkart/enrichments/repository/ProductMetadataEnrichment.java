package com.applicate.services.channelkart.enrichments.repository;

import com.applicate.services.channelkart.services.ProductMetaDataService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.ProductMetaData;

public class ProductMetadataEnrichment extends AbstractEnrichment<ProductMetaData> {

    private static final String ALL = "ALL";
    @Override
    public OperationResult.StepResult apply(ProductMetaData cdm) {

        ProductMetaDataService productMetaDataService = (ProductMetaDataService) ServiceLocator.lookup(ProductMetaData.class);

        if( !StringUtils.isEmpty(cdm.getChannel()) && cdm.getChannel().equalsIgnoreCase("all") ){
            cdm.setChannel(ALL);
        }

        if( !StringUtils.isEmpty(cdm.getSubChannel()) && cdm.getSubChannel().equalsIgnoreCase("all") ){
            cdm.setSubChannel(ALL);
        }

        if(cdm.getLoginid()==null){
            cdm.setLoginid(productMetaDataService.getLoginId(cdm.getBatchCode()));
        }

        return new OperationResult.StepResult(OperationResult.Status.OK,"Data enriched successfully");
    }
}
