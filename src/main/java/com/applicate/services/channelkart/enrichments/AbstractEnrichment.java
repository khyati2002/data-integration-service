package com.applicate.services.channelkart.enrichments;

public abstract class AbstractEnrichment<T> implements Enrichment{

    private EnrichmentInfo enrichmentInfo;

    public EnrichmentInfo getEnrichmentInfo() {
        return enrichmentInfo;
    }
    public void setEnrichmentInfo(EnrichmentInfo enrichmentInfo) {
        this.enrichmentInfo = enrichmentInfo;
    }
    protected AbstractEnrichment(EnrichmentInfo enrichmentInfo) {
        this.enrichmentInfo=enrichmentInfo;
    }
    protected AbstractEnrichment() {
        this(null);
    }
    public abstract EnrichmentResult apply(T cdm);
}
