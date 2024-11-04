package com.salescode.dataintegration.etl.enrichment;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.jooq.generated.tables.pojos.CkEnrichmentInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractEnrichment<T extends CommonDataModel> implements Enrichment<T> {

    private CkEnrichmentInfo enrichmentInfo;

}