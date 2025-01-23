package com.applicate.services.channelkart.enrichments.repository;

import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.models.OutletDetails;

public class TestEnrichment extends AbstractEnrichment<OutletDetails> {

    @Override
    public EnrichmentResult apply(OutletDetails cdm) {
        cdm.setEmail("dataintegration@salescode.ai");
        return EnrichmentResult.OK;
    }
}
