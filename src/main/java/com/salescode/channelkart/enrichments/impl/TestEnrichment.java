package com.salescode.channelkart.enrichments.impl;

import com.salescode.channelkart.models.OutletDetails;
import com.salescode.dataintegration.etl.enrichment.AbstractEnrichment;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Slf4j
public class TestEnrichment extends AbstractEnrichment<OutletDetails> {

    @Override
    public EnrichmentResult apply(OutletDetails cdm) {
        log.info("Enrichment started from bundle");
        cdm.setEmail("dataintegration@salescode.ai");
        EnrichmentResult enrichmentResult = new EnrichmentResult(EnrichmentResult.Status.OK);
        enrichmentResult.setEnrichedData(List.of(cdm));
        return enrichmentResult;
    }
}
