package com.salescode.dataintegration.etl.interfaces;

public interface TypeAwareEtlStep {

    EtlType getSourceType();

    enum EtlType {
        TRANSFORMER,
        VALIDATION,
        ENRICHMENT
    }
}
