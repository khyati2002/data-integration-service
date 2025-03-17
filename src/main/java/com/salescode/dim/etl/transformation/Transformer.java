package com.salescode.dim.etl.transformation;


import com.salescode.dim.interfaces.TypeAwareEtlStep;

public interface Transformer<S, T> extends TypeAwareEtlStep {

    T transform(S s) throws Exception;

    @Override
    default EtlType getSourceType() {
        return EtlType.TRANSFORMER;
    }
}
