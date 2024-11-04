package com.salescode.dataintegration.etl.enrichment;

import com.salescode.dataintegration.etl.interfaces.TypeAwareEtlStep;

public interface Enrichment<T> extends TypeAwareEtlStep {

    EnrichmentResult apply(T cdm);

    @Override
    default EtlType getSourceType() {
        return EtlType.ENRICHMENT;
    }
}
