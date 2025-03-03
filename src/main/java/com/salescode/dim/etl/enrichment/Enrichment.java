package com.salescode.dim.etl.enrichment;

import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.interfaces.TypeAwareEtlStep;

public interface Enrichment<T> extends TypeAwareEtlStep {

    EnrichmentResult apply(T cdm);

    @Override
    default EtlType getSourceType() {
        return EtlType.ENRICHMENT;
    }
}
