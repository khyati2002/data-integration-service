package com.salescode.dataintegration.etl.enrichment;

import com.salescode.channelkart.enrichments.EnrichmentInfo;
import com.salescode.channelkart.models.CommonDataModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractEnrichment<T extends CommonDataModel> implements Enrichment<T> {

    private EnrichmentInfo enrichmentInfo;

}