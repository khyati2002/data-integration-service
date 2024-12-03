package com.salescode.channelkart.enrichments.impl;

import com.salescode.dataintegration.etl.enrichment.AbstractEnrichment;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;
import com.salescode.jooq.generated.tables.pojos.CkOutletDetails;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Slf4j
public class TestEnrichment extends AbstractEnrichment<CkOutletDetails> {

    @Override
    public EnrichmentResult apply(CkOutletDetails cdm) {
        log.info("Enrichment started from bundle");
        cdm.setEmail("dataintegration@salescode.ai");
        EnrichmentResult enrichmentResult = new EnrichmentResult(EnrichmentResult.Status.OK);
        enrichmentResult.setEnrichedData(List.of(cdm));
        return enrichmentResult;
    }
}
