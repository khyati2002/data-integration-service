package com.salescode.channelkart.enrichments.impl;

import com.salescode.channelkart.enrichments.AbstractEnrichment;
import com.salescode.channelkart.enrichments.EnrichmentResult;
import com.salescode.channelkart.models.OutletDetails;
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
