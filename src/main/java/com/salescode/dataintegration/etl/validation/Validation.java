package com.salescode.dataintegration.etl.validation;

import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;
import com.salescode.dataintegration.etl.interfaces.TypeAwareEtlStep;

public interface Validation<T> extends TypeAwareEtlStep {

    RuleResult apply(T cdm);

    @Override
    default EtlType getSourceType() {
        return EtlType.VALIDATION;
    }
}
