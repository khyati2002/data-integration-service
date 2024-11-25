package com.salescode.dataintegration.etl.impl;

import com.salescode.dataintegration.etl.enrichment.AbstractEnrichment;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;
import com.salescode.jooq.CkOutletDetails;

import java.util.List;

public class TestEnrichment extends AbstractEnrichment<CkOutletDetails> {
    @Override
    public EnrichmentResult apply(CkOutletDetails cdm) {
        System.out.println("Enriching: " + cdm);
        cdm.setEmail("dataintegration@salescode.ai");
        EnrichmentResult enrichmentResult = new EnrichmentResult(EnrichmentResult.Status.OK);
        enrichmentResult.setEnrichedData(List.of(cdm));
        return enrichmentResult;
    }
}
