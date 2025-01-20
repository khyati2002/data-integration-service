package com.applicate.services.channelkart.enrichments.repository;

import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.models.OutletDetails;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestEnrichment extends AbstractEnrichment<OutletDetails> {

    @Override
    public EnrichmentResult apply(OutletDetails cdm) {
        log.info("Enrichment started from bundle");
        cdm.setEmail("dataintegration@salescode.ai");
        return EnrichmentResult.OK;
    }
}
