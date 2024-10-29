package com.salescode.dataintegration.etl.interfaces;

public interface TypeAwareEtlStep {

    EtlType getSourceType();

    enum EtlType {
        TRANSFORMER,
        PRE_VALIDATION_ENRICHMENT,
        VALIDATION,
        POST_VALIDATION_ENRICHMENT,
        PRE_SAVE_ENRICHMENT
    }
}
