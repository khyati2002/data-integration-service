package com.salescode.dim.interfaces;

public interface TypeAwareEtlStep {

    default void open(){};

    EtlType getSourceType();

    enum EtlType {
        TRANSFORMER,
        VALIDATION,
        ENRICHMENT
    }
}
