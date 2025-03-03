package com.salescode.dim.etl.enrichment;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.jooq.generated.tables.pojos.EnrichmentInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractEnrichment<T extends CommonDataModel> implements Enrichment<T> {

    private EnrichmentInfo enrichmentInfo;

}