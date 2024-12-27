package com.salescode.channelkart.enrichments;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class AbstractEnrichment<T> implements Enrichment {

    private EnrichmentInfo enrichmentInfo;

    protected AbstractEnrichment(EnrichmentInfo enrichmentInfo) {
        this.enrichmentInfo = enrichmentInfo;
    }

    protected AbstractEnrichment() {
        this(null);
    }

    public abstract EnrichmentResult apply(T cdm);
}
